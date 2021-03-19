package com.diozero.remote.message;

public class GpioGetPwmFrequency extends GpioBase {
	private static final long serialVersionUID = -1984694558846675394L;

	public GpioGetPwmFrequency(int gpio, String correlationId) {
		super(gpio, correlationId);
	}

	@Override
	public String toString() {
		return "GpioGetPwmFrequency [gpio=" + getGpio() + "]";
	}
}
