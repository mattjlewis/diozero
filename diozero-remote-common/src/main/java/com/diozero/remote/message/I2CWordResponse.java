package com.diozero.remote.message;

public class I2CWordResponse extends Response {
	private static final long serialVersionUID = 1095700116807205896L;

	private int data;
	
	public I2CWordResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public I2CWordResponse(int data, String correlationId) {
		this(Status.OK, null, data, correlationId);
	}
	
	public I2CWordResponse(Response.Status status, String detail, int data, String correlationId) {
		super(status, detail, correlationId);
		
		this.data = data;
	}

	public int getData() {
		return data;
	}
}
