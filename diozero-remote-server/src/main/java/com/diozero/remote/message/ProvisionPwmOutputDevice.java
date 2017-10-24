package com.diozero.remote.message;

public class ProvisionPwmOutputDevice extends GpioBase {
	private static final long serialVersionUID = 4305177018064043997L;

	private int frequency;
	private float initialValue;

	public ProvisionPwmOutputDevice(int gpio, int frequency, float initialValue, String correlationId) {
		super(gpio, correlationId);

		this.frequency = frequency;
		this.initialValue = initialValue;
	}

	public int getFrequency() {
		return frequency;
	}

	public float getInitialValue() {
		return initialValue;
	}
}
