package com.diozero.remote.message;

public abstract class GpioBase extends Request {
	private static final long serialVersionUID = -3472169312684611048L;

	private int gpio;

	public GpioBase(int gpio, String correlationId) {
		super(correlationId);
		
		this.gpio = gpio;
	}
	
	public int getGpio() {
		return gpio;
	}
}
