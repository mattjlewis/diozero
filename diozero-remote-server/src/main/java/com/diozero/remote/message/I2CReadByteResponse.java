package com.diozero.remote.message;

public class I2CReadByteResponse extends Response {
	private static final long serialVersionUID = 6206218402640384249L;

	private byte data;
	
	public I2CReadByteResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public I2CReadByteResponse(byte data, String correlationId) {
		this(Status.OK, null, data, correlationId);
	}
	
	public I2CReadByteResponse(Response.Status status, String detail, byte data, String correlationId) {
		super(status, detail, correlationId);
		
		this.data = data;
	}

	public byte getData() {
		return data;
	}
}
