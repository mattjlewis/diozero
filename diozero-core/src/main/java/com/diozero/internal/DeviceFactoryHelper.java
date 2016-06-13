package com.diozero.internal;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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


import java.util.ServiceLoader;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.util.DioZeroScheduler;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used.
 * Alternatively you can set the command line property "com.diozero.devicefactory" to override the ServiceLoader.
 */
public class DeviceFactoryHelper {
	private static final String SYSTEM_PROPERTY = "com.diozero.devicefactory";
	
	private static NativeDeviceFactoryInterface nativeDeviceFactory;

	private static void init() {
		synchronized (DeviceFactoryHelper.class) {
			if (nativeDeviceFactory == null) {
				// First try load one defined as a system property
				String property = System.getProperty(SYSTEM_PROPERTY);
				if (property != null && property.length() > 0) {
					try {
						nativeDeviceFactory = (NativeDeviceFactoryInterface)Class.forName(property).newInstance();
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
						Logger.error(e, "Cannot instantiate device factory class '{}'", property);
					}
				}
				
				// Otherwise use the ServiceLoader
				if (nativeDeviceFactory == null) {
					ServiceLoader<NativeDeviceFactoryInterface> service_loader = ServiceLoader.load(NativeDeviceFactoryInterface.class);
					for (NativeDeviceFactoryInterface device_provider_factory : service_loader) {
						nativeDeviceFactory = device_provider_factory;
						break;
					}
				}
				
				if (nativeDeviceFactory != null) {
					Logger.info("Using native device factory class {}", nativeDeviceFactory.getName());
					
					// TODO Load capabilities for the current native device (e.g. Raspberry Pi, ...)
					
					Runtime.getRuntime().addShutdownHook(new ShutdownHandlerThread(nativeDeviceFactory));
				}
			}
			
			if (nativeDeviceFactory == null) {
				throw new IllegalStateException("Error: no device provider factory service found,"
						+ " please configure META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface"
						+ " or set -Dcom.diozero.devicefactory");
			}
		}
	}
	
	public static NativeDeviceFactoryInterface getNativeDeviceFactory() {
		init();
		
		return nativeDeviceFactory;
	}

	public static void setNativeDeviceFactory(NativeDeviceFactoryInterface f) {
		synchronized (DeviceFactoryHelper.class) {
			if (nativeDeviceFactory != null)
				throw new IllegalStateException("Alreade initialized");
			nativeDeviceFactory = f;
		}
	}
}

class ShutdownHandlerThread extends Thread {
	private DeviceFactoryInterface deviceFactory;

	public ShutdownHandlerThread(DeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
		setName("DIO-Zero Shutdown Handler");
		setDaemon(false);
	}
	
	@Override
	public void run() {
		Logger.debug("Shutdown handler running");
		DioZeroScheduler.shutdownAll();
		deviceFactory.shutdown();
		Logger.debug("Shutdown handler finished");
	}
}
