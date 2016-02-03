package com.diozero;

import java.io.IOException;

import com.diozero.api.DigitalOutputDevice;

public class Buzzer extends DigitalOutputDevice {

	public Buzzer(int pinNumber) throws IOException {
		super(pinNumber);
	}

	public Buzzer(int pinNumber, boolean activeHigh) throws IOException {
		super(pinNumber, activeHigh, false);
	}
	
	public void beep() throws IOException {
		beep(1, 1, INFINITE_ITERATIONS, true);
	}
	
	public void beep(float onTime, float offTime, int n, boolean background) throws IOException {
		onOffLoop(onTime, offTime, n, background);
	}
	
	public boolean isActive() throws IOException {
		return isOn();
	}
}
