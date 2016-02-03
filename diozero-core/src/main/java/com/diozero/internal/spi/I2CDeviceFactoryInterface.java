package com.diozero.internal.spi;

import java.io.IOException;

public interface I2CDeviceFactoryInterface extends DeviceFactoryInterface {
	I2CDeviceInterface provisionI2CDevice(int controller, int address, int addressSize, int clockFrequency) throws IOException;
}
