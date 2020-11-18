package com.diozero.remote.message;

public class I2CReadBlockData extends I2CBase {
	private static final long serialVersionUID = -5749814542237023110L;

	private int register;

	public I2CReadBlockData(int controller, int address, int register, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
	}

	public int getRegister() {
		return register;
	}
}
