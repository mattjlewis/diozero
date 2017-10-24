package com.diozero.remote.message;

public class I2CWriteI2CBlockData extends I2CBase {
	private static final long serialVersionUID = 9186048619244155394L;

	private int register;
	private byte[] data;

	public I2CWriteI2CBlockData(int controller, int address, int register, byte[] data, String correlationId) {
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
