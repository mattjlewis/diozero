package com.diozero;

import java.io.IOException;

import com.diozero.api.PwmOutputDevice;

public class PwmLed extends PwmOutputDevice {
	public PwmLed(int pinNumber) throws IOException {
		this(pinNumber, 0);
	}
	
	public PwmLed(int pinNumber, float initialValue) throws IOException {
		super(pinNumber, initialValue);
	}
	
	public void blink() throws IOException {
		blink(1, 1, INFINITE_ITERATIONS, true);
	}
	
	public void blink(float onTime, float offTime, int iterations, boolean background) throws IOException {
		onOffLoop(onTime, offTime, iterations, background);
	}
	
	public void pulse() throws IOException {
		fadeInOutLoop(1, 50, INFINITE_ITERATIONS, true);
	}
	
	public void pulse(float fadeTime, int steps, int iterations, boolean background) throws IOException {
		fadeInOutLoop(fadeTime, steps, iterations, background);
	}
	
	public boolean isLit() throws IOException {
		return isOn();
	}
}
