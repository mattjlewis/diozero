package com.diozero.remote.message;

public class I2CReadBytes extends I2CBase {
	private static final long serialVersionUID = -4725936254997520160L;

	private int length;

	public I2CReadBytes(int controller, int address, int length, String correlationId) {
		super(controller, address, correlationId);
		
		this.length = length;
	}

	public int getLength() {
		return length;
	}
}
