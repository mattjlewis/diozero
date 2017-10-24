package com.diozero.remote.message;

import java.util.List;

public class GetBoardGpioInfoResponse extends Response {
	private List<GpioInfo> gpios;
	
	public GetBoardGpioInfoResponse(List<GpioInfo> gpios, String correlationId) {
		super(Status.OK, null, correlationId);
		
		this.gpios = gpios;
	}
	
	public GetBoardGpioInfoResponse(Status status, String detail, List<GpioInfo> gpios, String correlationId) {
		super(status, detail, correlationId);
		
		this.gpios = gpios;
	}

	public List<GpioInfo> getGpios() {
		return gpios;
	}
}
