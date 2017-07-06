package com.diozero.api;

import com.diozero.util.RuntimeIOException;

public class InvalidModeException extends RuntimeIOException {
	private static final long serialVersionUID = 5073118365701122269L;

	public InvalidModeException(String message) {
		super(message);
	}
}
