package com.diozero.remote.message;

public class I2CWrite extends I2CBase {
	private static final long serialVersionUID = 7030568493963170357L;

	private byte[] data;
	
	public I2CWrite(int controller, int address, byte[] data, String correlationId) {
		super(controller, address, correlationId);
		
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}
}
