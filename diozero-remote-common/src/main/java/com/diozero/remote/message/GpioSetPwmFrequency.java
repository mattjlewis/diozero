package com.diozero.remote.message;

public class GpioSetPwmFrequency extends GpioBase {
	private static final long serialVersionUID = -3428782325652376734L;

	private int frequency;

	public GpioSetPwmFrequency(int gpio, int frequency, String correlationId) {
		super(gpio, correlationId);
		
		this.frequency = frequency;
	}

	public int getFrequency() {
		return frequency;
	}

	@Override
	public String toString() {
		return "GpioSetPwmFrequency [frequency=" + frequency + ", gpio=" + getGpio() + "]";
	}
}
