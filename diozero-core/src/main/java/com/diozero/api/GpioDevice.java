package com.diozero.api;

import java.io.Closeable;

public abstract class GpioDevice implements Closeable {
	protected int pinNumber;
	
	public GpioDevice(int pinNumber) {
		this.pinNumber = pinNumber;
	}
	
	public int getPinNumber() {
		return pinNumber;
	}
}
