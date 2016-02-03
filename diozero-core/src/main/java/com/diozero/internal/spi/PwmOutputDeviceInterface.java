package com.diozero.internal.spi;

import java.io.IOException;

public interface PwmOutputDeviceInterface extends DeviceInterface {
	@Override
	void close();
	int getPin();
	float getValue() throws IOException;
	void setValue(float value) throws IOException;
}
