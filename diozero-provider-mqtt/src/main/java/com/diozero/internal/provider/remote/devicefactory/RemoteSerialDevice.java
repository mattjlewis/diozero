package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Provider
 * Filename:     RemoteSerialDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.UUID;

import org.tinylog.Logger;

import com.diozero.api.SerialDevice;
import com.diozero.api.SerialDeviceInterface;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SerialBytesAvailable;
import com.diozero.remote.message.SerialBytesAvailableResponse;
import com.diozero.remote.message.SerialClose;
import com.diozero.remote.message.SerialOpen;
import com.diozero.remote.message.SerialRead;
import com.diozero.remote.message.SerialReadByte;
import com.diozero.remote.message.SerialReadByteResponse;
import com.diozero.remote.message.SerialReadBytes;
import com.diozero.remote.message.SerialReadBytesResponse;
import com.diozero.remote.message.SerialReadResponse;
import com.diozero.remote.message.SerialWriteByte;
import com.diozero.remote.message.SerialWriteBytes;
import com.diozero.util.RuntimeIOException;

public class RemoteSerialDevice extends AbstractDevice implements SerialDeviceInterface {
	private RemoteDeviceFactory deviceFactory;
	private String deviceFile;

	public RemoteSerialDevice(RemoteDeviceFactory deviceFactory, String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		this.deviceFile = deviceFile;

		SerialOpen request = new SerialOpen(deviceFile, baud, dataBits, stopBits, parity, readBlocking, minReadChars,
				readTimeoutMillis, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error: " + response.getDetail());
		}
	}

	@Override
	public int read() {
		SerialRead request = new SerialRead(deviceFile, UUID.randomUUID().toString());

		SerialReadResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in Serial readByte: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		SerialReadByte request = new SerialReadByte(deviceFile, UUID.randomUUID().toString());

		SerialReadByteResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in Serial readByte: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public void writeByte(byte bVal) {
		SerialWriteByte request = new SerialWriteByte(deviceFile, bVal, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in Serial writeByte: " + response.getDetail());
		}
	}

	@Override
	public int read(byte[] buffer) {
		SerialReadBytes request = new SerialReadBytes(deviceFile, buffer.length, UUID.randomUUID().toString());

		SerialReadBytesResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in Serial read: " + response.getDetail());
		}

		byte[] result = response.getData();
		System.arraycopy(result, 0, buffer, 0, result.length);
		
		return result.length;
	}

	@Override
	public void write(byte[] data) {
		SerialWriteBytes request = new SerialWriteBytes(deviceFile, data, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in Serial write: " + response.getDetail());
		}
	}

	@Override
	public int bytesAvailable() {
		SerialBytesAvailable request = new SerialBytesAvailable(deviceFile, UUID.randomUUID().toString());

		SerialBytesAvailableResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in Serial bytesAvailable: " + response.getDetail());
		}

		return response.getBytesAvailable();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		SerialClose request = new SerialClose(deviceFile, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			Logger.error("Error closing device: " + response.getDetail());
		}
	}
}
