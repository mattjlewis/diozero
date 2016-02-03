package com.diozero.internal.spi;

import java.io.IOException;

public interface GpioDigitalOutputDeviceInterface extends GpioDeviceInterface {
	@Override
	void close();
	boolean getValue() throws IOException;
	void setValue(boolean value) throws IOException;
}
