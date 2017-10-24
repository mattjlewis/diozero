package com.diozero.remote.message;

public class I2COpen extends I2CBase {
	private static final long serialVersionUID = -1707373552203874175L;

	private int addressSize;
	private int clockFrequency;

	public I2COpen(int controller, int address, int addressSize, int clockFrequency, String correlationId) {
		super(controller, address, correlationId);
		
		this.addressSize = addressSize;
		this.clockFrequency = clockFrequency;
	}

	public int getAddressSize() {
		return addressSize;
	}

	public int getClockFrequency() {
		return clockFrequency;
	}
}
