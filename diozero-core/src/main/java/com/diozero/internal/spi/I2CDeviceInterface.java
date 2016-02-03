package com.diozero.internal.spi;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface I2CDeviceInterface extends DeviceInterface {
	void read(int register, int subAddressSize, ByteBuffer buffer) throws IOException;
	void write(int register, int subAddressSize, ByteBuffer buffer) throws IOException;
}
