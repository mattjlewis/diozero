package com.diozero.internal.spi;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface SpiDeviceInterface extends DeviceInterface {
	ByteBuffer writeAndRead(ByteBuffer out) throws IOException;
	int getController();
	int getChipSelect();
}
