package com.diozero.api;

public interface DeviceInterface extends AutoCloseable {
	/**
	 * Close this device
	 */
	@Override
	void close() throws RuntimeIOException;
}
