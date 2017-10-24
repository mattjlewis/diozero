package com.diozero.remote.message;

public class I2CReadByteData extends I2CBase {
	private static final long serialVersionUID = 7828733154277234842L;

	private int register;
	
	public I2CReadByteData(int controller, int address, int register, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
	}

	public int getRegister() {
		return register;
	}
}
