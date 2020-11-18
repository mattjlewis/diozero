package com.diozero.remote.message;

public class I2CWriteBytes extends I2CBase {
	private static final long serialVersionUID = 6558101835819516911L;

	private byte[] data;

	public I2CWriteBytes(int controller, int address, byte[] data, String correlationId) {
		super(controller, address, correlationId);
		
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}
}
