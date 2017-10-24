package com.diozero.remote.message;

public class I2CWriteByteData extends I2CBase {
	private static final long serialVersionUID = -4858919784582680629L;
	
	private int register;
	private byte data;
	
	public I2CWriteByteData(int controller, int address, int register, byte data, String correlationId) {
		super(controller, address, correlationId);
		
		this.register = register;
		this.data = data;
	}
	
	public int getRegister() {
		return register;
	}

	public byte getData() {
		return data;
	}
}
