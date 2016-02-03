package com.diozero.internal.spi;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractDevice implements DeviceInterface {
	private static final Logger logger = LogManager.getLogger(AbstractDevice.class);
	
	private String key;
	private DeviceFactoryInterface deviceFactory;
	
	public AbstractDevice(String key, DeviceFactoryInterface deviceFactory) {
		this.key = key;
		this.deviceFactory = deviceFactory;
	}
	
	@Override
	public final String getKey() {
		return key;
	}
	
	@Override
	public boolean isOpen() {
		return deviceFactory.isDeviceOpened(key);
	}
	
	@Override
	public final void close() {
		logger.debug("close()");
		try {
			closeDevice();
		} catch (IOException e) {
			logger.error("Error closing device: " + e, e);
		}
		deviceFactory.deviceClosed(this);
	}
	
	protected abstract void closeDevice() throws IOException;
}
