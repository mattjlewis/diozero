package com.diozero.internal.spi;

import java.io.Closeable;

public interface DeviceInterface extends Closeable {
	String getKey();
	public boolean isOpen();
}
