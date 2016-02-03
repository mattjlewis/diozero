package com.diozero;

import java.io.IOException;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;

public class Button extends DigitalInputDevice {

	public Button(int pinNumber) throws IOException {
		super(pinNumber, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	public Button(int pinNumber, GpioPullUpDown pud) throws IOException {
		super(pinNumber, pud, GpioEventTrigger.BOTH);
	}
}
