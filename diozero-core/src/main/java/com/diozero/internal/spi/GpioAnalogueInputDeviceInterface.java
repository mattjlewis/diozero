package com.diozero.internal.spi;

import java.io.IOException;

public interface GpioAnalogueInputDeviceInterface extends DeviceInterface {
	@Override
	void close();
	float getValue() throws IOException;
	int getPin();
}
