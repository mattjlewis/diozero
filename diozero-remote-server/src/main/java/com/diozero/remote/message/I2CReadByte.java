package com.diozero.remote.message;

public class I2CReadByte extends I2CBase {
	private static final long serialVersionUID = 20265863691075770L;

	public I2CReadByte(int controller, int address, String correlationId) {
		super(controller, address, correlationId);
	}
}
