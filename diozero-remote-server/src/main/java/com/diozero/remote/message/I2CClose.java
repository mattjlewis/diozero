package com.diozero.remote.message;

public class I2CClose extends I2CBase {
	private static final long serialVersionUID = 3991444444044813261L;

	public I2CClose(int controller, int address, String correlationId) {
		super(controller, address, correlationId);
	}
}
