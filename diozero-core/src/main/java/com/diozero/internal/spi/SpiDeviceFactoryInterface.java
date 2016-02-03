package com.diozero.internal.spi;

import java.io.IOException;

import com.diozero.api.SpiClockMode;

public interface SpiDeviceFactoryInterface extends DeviceFactoryInterface {
	SpiDeviceInterface provisionSpiDevice(int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws IOException;
}
