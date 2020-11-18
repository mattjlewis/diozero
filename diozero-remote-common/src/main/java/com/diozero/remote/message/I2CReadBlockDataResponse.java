package com.diozero.remote.message;

public class I2CReadBlockDataResponse extends Response {
	private static final long serialVersionUID = 2797160651448319434L;

	private int bytesRead;
	private byte[] data;
	
	public I2CReadBlockDataResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public I2CReadBlockDataResponse(int bytesRead, byte[] data, String correlationId) {
		this(Status.OK, null, bytesRead, data, correlationId);
	}
	
	public I2CReadBlockDataResponse(Response.Status status, String detail, int bytesRead, byte[] data, String correlationId) {
		super(status, detail, correlationId);
		
		this.bytesRead = bytesRead;
		this.data = data;
	}

	public int getBytesRead() {
		return bytesRead;
	}

	public byte[] getData() {
		return data;
	}
}
