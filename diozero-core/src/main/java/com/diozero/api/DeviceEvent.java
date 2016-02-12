package com.diozero.api;

public abstract class DeviceEvent {
	private int pin;
	private long epochTime;
	private long nanoTime;
	
	public DeviceEvent(int pin, long epochTime, long nanoTime) {
		this.pin = pin;
		this.epochTime = epochTime;
		this.nanoTime = nanoTime;
	}

	public int getPin() {
		return pin;
	}

	public long getEpochTime() {
		return epochTime;
	}

	public long getNanoTime() {
		return nanoTime;
	}
}
