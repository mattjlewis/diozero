package com.diozero.api;

import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used.
 * Alternatively you can set the command line property "com.diozero.devicefactory" to override the ServiceLoader.
 */
public class DeviceFactoryHelper {
	private static final Logger logger = LogManager.getLogger(DeviceFactoryHelper.class);

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
						logger.error("Cannot instantiate device factory class '" + property + "'", e);
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
					logger.info("Using native device factory class " + nativeDeviceFactory.getName());
					
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
}

class ShutdownHandlerThread extends Thread {
	private DeviceFactoryInterface deviceFactory;

	public ShutdownHandlerThread(DeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
		setName("DIO-Zero Shutdown Handler");
	}
	
	@Override
	public void run() {
		deviceFactory.closeAll();
	}
}
