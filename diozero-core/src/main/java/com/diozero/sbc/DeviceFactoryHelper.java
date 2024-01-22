package com.diozero.sbc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DeviceFactoryHelper.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import org.tinylog.Logger;

import com.diozero.internal.provider.builtin.DefaultDeviceFactory;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.util.Diozero;
import com.diozero.util.PropertyUtil;
import com.diozero.util.StringUtil;

/**
 * Helper class for automatically detecting the
 * {@link NativeDeviceFactoryInterface native device factory} that will be used
 * for provisioning I/O devices. Uses the Java
 * {@link java.util.ServiceLoader#load ServiceLoader} to detect
 * NativeDeviceFactoryInterface implementations that are available on the
 * class-path as defined in
 * <code>META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface</code>.
 * The first entry is used if there are multiple implementations present on the
 * class-path. Alternatively you can specify the class to use via the command
 * line or environment property <code>diozero.devicefactory</code>. The
 * {@link com.diozero.internal.provider.builtin.DefaultDeviceFactory built-in
 * device factory} is used if the above logic fails to instantiate a device
 * factory implementation.
 */
public class DeviceFactoryHelper {
	public static final String DEVICE_FACTORY_PROP = "diozero.devicefactory";

	private static NativeDeviceFactoryInterface nativeDeviceFactory;

	private static synchronized void initialise() {
		if (nativeDeviceFactory == null) {
			// First try load one defined as a system property
			String property = PropertyUtil.getProperty(DEVICE_FACTORY_PROP, null);
			if (StringUtil.isNotBlank(property)) {
				try {
					nativeDeviceFactory = (NativeDeviceFactoryInterface) Class.forName(property)
							.getDeclaredConstructor().newInstance();
				} catch (ReflectiveOperationException e) {
					Logger.error(e, "Cannot instantiate device factory class '{}'", property);
				}
			}

			// Otherwise use the ServiceLoader
			// If none found use the default built-in device factory
			if (nativeDeviceFactory == null) {
				nativeDeviceFactory = NativeDeviceFactoryInterface.loadInstances().findFirst()
						.orElseGet(() -> new DefaultDeviceFactory());
			}

			Logger.debug("Using native device factory class {}", nativeDeviceFactory.getClass().getSimpleName());

			Diozero.initialiseShutdownHook();

			nativeDeviceFactory.start();
		} else if (nativeDeviceFactory.isClosed()) {
			nativeDeviceFactory.reopen();
		}
	}

	/**
	 * Auto-detect the native device factory class to be used for provisioning I/O
	 * devices in this lookup order:
	 * <ol>
	 * <li>Command line {@link PropertyUtil#getProperty(String, String) property /
	 * environment} variable <code>diozero.devicefactory</code></li>
	 * <li>Java {@link java.util.ServiceLoader#load ServiceLoader} using the class
	 * <code>com.diozero.internal.provider.NativeDeviceFactoryInterface</code></li>
	 * <li>The {@link com.diozero.internal.provider.builtin.DefaultDeviceFactory
	 * built-in device factory}</li>
	 * </ol>
	 *
	 * @return the native device factory instance to use for provisioning I/O
	 *         devices.
	 */
	public static NativeDeviceFactoryInterface getNativeDeviceFactory() {
		initialise();

		return nativeDeviceFactory;
	}

	/**
	 * Register an object to be shutdown in the case of abnormal shutdown
	 *
	 * @param closeableArray Array of closeable objects to close on shutdown
	 *
	 * @deprecated Use {@link Diozero#registerForShutdown} instead
	 */
	@Deprecated
	public static void registerForShutdown(AutoCloseable... closeableArray) {
		Diozero.registerForShutdown(closeableArray);
	}

	/**
	 * Shutdown diozero.
	 *
	 * @deprecated Use {@link Diozero#shutdown} instead
	 */
	@Deprecated
	public static void shutdown() {
		Diozero.shutdown();
	}

	/**
	 * Close the default native device factory.
	 */
	public static void close() {
		// Close all InternalDeviceInterface instances that are still open
		if (nativeDeviceFactory != null && !nativeDeviceFactory.isClosed()) {
			nativeDeviceFactory.close();
		}
	}
}
