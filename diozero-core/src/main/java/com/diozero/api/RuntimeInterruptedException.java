package com.diozero.api;

public class RuntimeInterruptedException extends RuntimeIOException {
	private static final long serialVersionUID = 5886751281157736108L;

	public RuntimeInterruptedException(Exception e) {
		super(e);
	}
}
