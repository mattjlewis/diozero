package com.diozero.remote.message;

public class I2CWriteByte extends I2CBase {
	private static final long serialVersionUID = 1555644537472217562L;

	private byte data;
	
	public I2CWriteByte(int controller, int address, byte data, String correlationId) {
		super(controller, address, correlationId);
		
		this.data = data;
	}

	public byte getData() {
		return data;
	}
}
