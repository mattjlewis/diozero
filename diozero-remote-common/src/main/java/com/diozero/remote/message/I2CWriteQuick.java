package com.diozero.remote.message;

public class I2CWriteQuick extends I2CBase {
	private static final long serialVersionUID = 7555840159259134061L;

	private int bit;
	
	public I2CWriteQuick(int controller, int address, int bit, String correlationId) {
		super(controller, address, correlationId);
		
		this.bit = bit;
	}

	public int getBit() {
		return bit;
	}
}
