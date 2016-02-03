package com.diozero.internal.spi;

import java.io.IOException;

public interface AnalogueInputDeviceFactoryInterface extends DeviceFactoryInterface {
	GpioAnalogueInputDeviceInterface provisionAnalogueInputPin(int pinNumber) throws IOException;
}
