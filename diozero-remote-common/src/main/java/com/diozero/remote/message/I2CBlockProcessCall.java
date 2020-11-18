package com.diozero.remote.message;

public class I2CBlockProcessCall extends I2CBase {
	private static final long serialVersionUID = 6056164478316188600L;

	private int register;
	private byte[] data;

	public I2CBlockProcessCall(int controller, int address, int register, byte[] data, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
		this.data = data;
	}

	public int getRegister() {
		return register;
	}

	public byte[] getData() {
		return data;
	}
}
