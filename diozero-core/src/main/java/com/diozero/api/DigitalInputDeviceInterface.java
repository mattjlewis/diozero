package com.diozero.api;

import com.diozero.util.RuntimeIOException;

public interface DigitalInputDeviceInterface {
	public boolean getValue() throws RuntimeIOException;
	public void close() throws RuntimeIOException;
}
