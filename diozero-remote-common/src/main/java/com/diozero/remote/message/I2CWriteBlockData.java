package com.diozero.remote.message;

public class I2CWriteBlockData extends I2CBase {
	private static final long serialVersionUID = -3517253931340451180L;

	private int register;
	private byte[] data;

	public I2CWriteBlockData(int controller, int address, int register, byte[] data, String correlationId) {
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
