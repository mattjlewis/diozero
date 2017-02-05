package com.diozero;

import java.io.Closeable;

public interface GpioExpander extends Closeable {
	void setDirections(int port, byte directions);
	void setValues(int port, byte values);
}
