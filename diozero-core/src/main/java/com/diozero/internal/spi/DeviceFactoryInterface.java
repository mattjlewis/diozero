package com.diozero.internal.spi;

public interface DeviceFactoryInterface {
	String getName();
	void closeAll();
	boolean isDeviceOpened(String key);
	void deviceOpened(DeviceInterface device);
	void deviceClosed(DeviceInterface device);
}
