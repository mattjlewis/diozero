package com.diozero.remote.message;

public class I2CBytesResponse extends Response {
	private static final long serialVersionUID = 7380391890590153327L;
	
	private byte[] data;
	
	public I2CBytesResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public I2CBytesResponse(byte[] data, String correlationId) {
		this(Status.OK, null, data, correlationId);
	}
	
	public I2CBytesResponse(Response.Status status, String detail, byte[] data, String correlationId) {
		super(status, detail, correlationId);
		
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}
}
