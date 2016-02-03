package com.diozero.internal.spi;

import java.io.IOException;

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;

public interface GpioDeviceFactoryInterface extends DeviceFactoryInterface {
	GpioDigitalInputDeviceInterface provisionDigitalInputPin(int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException;
	GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int pinNumber, boolean initialValue) throws IOException;
}
