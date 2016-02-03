package com.diozero.api;

public class DigitalPinEvent {
	private int pin;
	private long epochTime;
	private long nanoTime;
	private boolean value;

	public DigitalPinEvent(int pin, long epochTime, long nanoTime, boolean value) {
		this.pin = pin;
		this.epochTime = epochTime;
		this.nanoTime = nanoTime;
		this.value = value;
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

	public boolean getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "DigitalPinEvent [pin=" + pin + ", epochTime=" + epochTime + ", nanoTime=" + nanoTime + ", value="
				+ value + "]";
	}
}
