package com.diozero.remote.message;

public class I2CReadResponse extends Response {
	private static final long serialVersionUID = 7643237824383471955L;

	private byte[] data;
	
	public I2CReadResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public I2CReadResponse(byte[] data, String correlationId) {
		this(Status.OK, null, data, correlationId);
	}
	
	public I2CReadResponse(Response.Status status, String detail, byte[] data, String correlationId) {
		super(status, detail, correlationId);
		
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}
}
