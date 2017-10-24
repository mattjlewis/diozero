package com.diozero.remote.message;

public class I2CReadI2CBlockData extends I2CBase {
	private static final long serialVersionUID = -4619708096918301017L;

	private int register;
	private int length;

	public I2CReadI2CBlockData(int controller, int address, int register, int length, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
		this.length = length;
	}

	public int getRegister() {
		return register;
	}

	public int getLength() {
		return length;
	}
}
