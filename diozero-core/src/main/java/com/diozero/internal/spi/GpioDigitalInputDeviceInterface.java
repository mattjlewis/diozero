package com.diozero.internal.spi;

import java.io.IOException;

public interface GpioDigitalInputDeviceInterface extends GpioDeviceInterface {
	@Override
	void close();
	boolean getValue() throws IOException;
	void setDebounceTimeMillis(int debounceTime);
	void setListener(InternalPinListener listener);
	void removeListener();
}
