package com.diozero.internal.spi;

import java.io.IOException;

public interface PwmOutputDeviceFactoryInterface extends DeviceFactoryInterface {
	PwmOutputDeviceInterface provisionPwmOutputPin(int pinNumber, float initialValue) throws IOException;
}
