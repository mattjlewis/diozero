package com.diozero.remote.message;

public class I2CRead extends I2CBase {
	private static final long serialVersionUID = 859839169399727588L;
	
	private int length;

	public I2CRead(int controller, int address, int length, String correlationId) {
		super(controller, address, correlationId);
		
		this.length = length;
	}

	public int getLength() {
		return length;
	}
}
