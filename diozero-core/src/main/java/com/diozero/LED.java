package com.diozero;

import java.io.IOException;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

public class LED extends DigitalOutputDevice {

	public LED(int pinNumber) throws IOException {
		super(pinNumber);
	}

	public LED(int pinNumber, boolean activeHigh) throws IOException {
		super(pinNumber, activeHigh, false);
	}
	
	public LED(GpioDigitalOutputDeviceInterface device, boolean activeHigh) {
		super(device, activeHigh);
	}

	public void blink() throws IOException {
		blink(1, 1, INFINITE_ITERATIONS, true);
	}
	
	public void blink(float onTime, float offTime, int n, boolean background) throws IOException {
		onOffLoop(onTime, offTime, n, background);
	}
	
	public boolean isLit() throws IOException {
		return isOn();
	}
}
