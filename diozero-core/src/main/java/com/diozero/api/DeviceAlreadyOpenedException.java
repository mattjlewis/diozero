package com.diozero.api;


public class DeviceAlreadyOpenedException extends RuntimeException {
	private static final long serialVersionUID = 4497456846858554237L;

	public DeviceAlreadyOpenedException(String message) {
		super(message);
	}
}
