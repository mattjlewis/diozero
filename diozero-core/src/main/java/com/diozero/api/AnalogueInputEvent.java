package com.diozero.api;

public class AnalogueInputEvent extends DeviceEvent {
	private float value;
	
	public AnalogueInputEvent(int pin, long epochTime, long nanoTime, float value) {
		super(pin, epochTime, nanoTime);
		
		this.value = value;
	}
	
	public float getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "AnaloguePinEvent [pin=" + getPin() + ", epochTime=" + getEpochTime() +
				", nanoTime=" + getNanoTime() + ", value=" + value + "]";
	}
}
