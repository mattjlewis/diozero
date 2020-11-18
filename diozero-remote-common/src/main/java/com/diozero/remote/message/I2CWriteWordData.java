package com.diozero.remote.message;

public class I2CWriteWordData extends I2CBase {
	private static final long serialVersionUID = 6317420676419976544L;

	private int register;
	private int data;

	public I2CWriteWordData(int controller, int address, int register, int data, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
		this.data = data;
	}

	public int getData() {
		return data;
	}

	public int getRegister() {
		return register;
	}
}
