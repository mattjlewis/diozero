package com.diozero;

import java.io.IOException;

import com.diozero.api.DebouncedDigitalInputDevice;
import com.diozero.api.GpioPullUpDown;

public class LineSensor extends DebouncedDigitalInputDevice {

	public LineSensor(int pinNumber) throws IOException {
		super(pinNumber, GpioPullUpDown.NONE, 0);
	}

	public LineSensor(int pinNumber, GpioPullUpDown pud, float bounceTime) throws IOException {
		super(pinNumber, pud, bounceTime);
	}
}
