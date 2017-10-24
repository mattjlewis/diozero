package com.diozero.remote.message;

public abstract class I2CBase extends Request {
	private static final long serialVersionUID = -3644591853086169908L;
	
	private int controller;
	private int address;

	public I2CBase(int controller, int address, String correlationId) {
		super(correlationId);
		
		this.controller = controller;
		this.address = address;
	}

	public int getController() {
		return controller;
	}

	public int getAddress() {
		return address;
	}
}
