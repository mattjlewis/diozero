package com.diozero.internal.spi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.DeviceStates;

public abstract class AbstractDeviceFactory implements DeviceFactoryInterface {
	private static final Logger logger = LogManager.getLogger(AbstractDeviceFactory.class);
	
	private DeviceStates deviceStates;
	
	public AbstractDeviceFactory() {
		deviceStates = new DeviceStates();
	}
	
	@Override
	public final void closeAll() {
		logger.debug("closeAll()");
		deviceStates.closeAll();
	}
	
	@Override
	public final void deviceOpened(DeviceInterface device) {
		deviceStates.opened(device);
	}
	
	@Override
	public final void deviceClosed(DeviceInterface device) {
		deviceStates.closed(device);
	}
	
	@Override
	public final boolean isDeviceOpened(String key) {
		return deviceStates.isOpened(key);
	}

	@SuppressWarnings("unchecked")
	public final <T extends DeviceInterface> T getDevice(String key, Class<T> clz) {
		return (T)deviceStates.getDevice(key);
	}
}
