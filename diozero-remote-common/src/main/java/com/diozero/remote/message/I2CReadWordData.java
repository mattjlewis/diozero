package com.diozero.remote.message;

public class I2CReadWordData extends I2CBase {
	private static final long serialVersionUID = 6566108510150389873L;

	private int register;

	public I2CReadWordData(int controller, int address, int register, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
	}

	public int getRegister() {
		return register;
	}
}
