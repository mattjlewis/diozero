package com.diozero.api;

import java.io.Closeable;

public interface DeviceInterface extends Closeable {
	/**
	 * Close this device
	 */
	@Override
	void close();
}
