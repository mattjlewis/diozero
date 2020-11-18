package com.diozero.remote.message;

public class I2CBooleanResponse extends Response {
	private static final long serialVersionUID = 3289518782395068003L;
	
	private boolean result;
	
	public I2CBooleanResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public I2CBooleanResponse(boolean result, String correlationId) {
		this(Status.OK, null, result, correlationId);
	}
	
	public I2CBooleanResponse(Response.Status status, String detail, boolean result, String correlationId) {
		super(status, detail, correlationId);
		
		this.result = result;
	}

	public boolean getResult() {
		return result;
	}
}
