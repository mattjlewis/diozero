package com.diozero.remote.message;

public class GpioGetPwmFrequencyResponse extends Response {
	private static final long serialVersionUID = -8900787481430184176L;

	private int frequency;

	public GpioGetPwmFrequencyResponse(int frequency, String correlationId) {
		super(Response.Status.OK, null, correlationId);

		this.frequency = frequency;
	}

	public GpioGetPwmFrequencyResponse(String detail, String correlationId) {
		super(Response.Status.ERROR, detail, correlationId);
	}

	public GpioGetPwmFrequencyResponse(Response.Status status, String detail, int frequency, String correlationId) {
		super(status, detail, correlationId);

		this.frequency = frequency;
	}

	public int getFrequency() {
		return frequency;
	}

	@Override
	public String toString() {
		return "GpioGetPwmFrequencyResponse [frequency=" + frequency + ", status=" + getStatus() + ", detail()="
				+ getDetail() + "]";
	}
}
