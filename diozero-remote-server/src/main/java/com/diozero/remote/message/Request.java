package com.diozero.remote.message;

import java.io.Serializable;

public abstract class Request implements Serializable {
	private static final long serialVersionUID = -6638075900775727572L;
	
	private String correlationId;

	public Request(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}
}
