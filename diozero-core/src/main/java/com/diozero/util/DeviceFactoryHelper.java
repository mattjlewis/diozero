package com.diozero.util;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     DeviceFactoryHelper.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.sysfs.SysFsDeviceFactory;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used.
 * Alternatively you can set the command line property "com.diozero.devicefactory" to override the ServiceLoader.
 */
public class DeviceFactoryHelper {
	private static final String SYSTEM_PROPERTY = "com.diozero.devicefactory";
	
	private static NativeDeviceFactoryInterface nativeDeviceFactory;

	@SuppressWarnings("resource")
	private static void init() {
		synchronized (DeviceFactoryHelper.class) {
			if (nativeDeviceFactory == null) {
				// First try load one defined as a system property
				String property = System.getProperty(SYSTEM_PROPERTY);
				if (property != null && property.length() > 0) {
					try {
						nativeDeviceFactory = (NativeDeviceFactoryInterface) Class.forName(property)
								.getDeclaredConstructor().newInstance();
					} catch (ReflectiveOperationException e) {
						Logger.error(e, "Cannot instantiate device factory class '{}'", property);
					}
				}
				
				// Otherwise use the ServiceLoader
				// If none found use the universal sysfs device factory
				if (nativeDeviceFactory == null) {
					nativeDeviceFactory = NativeDeviceFactoryInterface.loadInstances().findFirst()
							.orElse(new SysFsDeviceFactory());
				}

				Logger.info("Using native device factory class {}", nativeDeviceFactory.getClass().getSimpleName());
					
				Runtime.getRuntime().addShutdownHook(new ShutdownHandlerThread(nativeDeviceFactory));
			}
			
			if (nativeDeviceFactory == null) {
				// Shouldn't be possible
				throw new IllegalStateException("Error: failed to load native device factory,"
						+ " please configure META-INF/services/" + NativeDeviceFactoryInterface.class.getName()
						+ " or set -D" + SYSTEM_PROPERTY);
			}
		}
	}
	
	public static NativeDeviceFactoryInterface getNativeDeviceFactory() {
		init();
		
		return nativeDeviceFactory;
	}

	public static void setNativeDeviceFactory(NativeDeviceFactoryInterface ndf) {
		synchronized (DeviceFactoryHelper.class) {
			if (nativeDeviceFactory != null) {
				throw new IllegalStateException("Already initialised");
			}
			nativeDeviceFactory = ndf;
		}
	}
}

class ShutdownHandlerThread extends Thread {
	private NativeDeviceFactoryInterface deviceFactory;

	public ShutdownHandlerThread(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
		setName("DIO-Zero Shutdown Handler");
		setDaemon(false);
	}
	
	@Override
	public void run() {
		Logger.debug("Shutdown handler running");
		deviceFactory.close();
		Logger.debug("Shutdown handler finished");
	}
}
