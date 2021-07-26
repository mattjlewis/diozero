package com.diozero.internal.provider.firmata.adapter;

public interface FirmataTransport extends AutoCloseable {
	int bytesAvailable();

	int read();

	byte readByte();

	void write(byte[] data);

	@Override
	void close();
}
