package com.diozero.remote.message;

public class I2CProcessCall extends I2CBase {
	private static final long serialVersionUID = 5492159724116594531L;

	private int register;
	private int data;

	public I2CProcessCall(int controller, int address, int register, int data, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
		this.data = data;
	}

	public int getRegister() {
		return register;
	}

	public int getData() {
		return data;
	}
}
