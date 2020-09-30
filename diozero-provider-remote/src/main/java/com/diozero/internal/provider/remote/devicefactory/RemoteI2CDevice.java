package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     RemoteI2CDevice.java  
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

import java.nio.ByteBuffer;
import java.util.UUID;

import org.tinylog.Logger;

import com.diozero.api.I2CDevice.ProbeMode;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CRead;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadByteResponse;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadResponse;
import com.diozero.remote.message.I2CWrite;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.Response;
import com.diozero.util.RuntimeIOException;

public class RemoteI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private RemoteDeviceFactory deviceFactory;
	private int controller;
	private int address;

	public RemoteI2CDevice(RemoteDeviceFactory deviceFactory, String key, int controller, int address, int addressSize,
			int clockFrequency) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		this.controller = controller;
		this.address = address;

		I2COpen request = new I2COpen(controller, address, addressSize, clockFrequency,
				UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error: " + response.getDetail());
		}
	}

	@Override
	public boolean probe(ProbeMode mode) throws RuntimeIOException {
		return false;
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		I2CReadByte request = new I2CReadByte(controller, address, UUID.randomUUID().toString());

		I2CReadByteResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readByte: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		I2CWriteByte request = new I2CWriteByte(controller, address, b, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeByte: " + response.getDetail());
		}
	}

	@Override
	public void read(ByteBuffer buffer) throws RuntimeIOException {
		I2CRead request = new I2CRead(controller, address, buffer.remaining(), UUID.randomUUID().toString());

		I2CReadResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C read: " + response.getDetail());
		}

		buffer.put(response.getData());
		buffer.flip();
	}

	@Override
	public void write(ByteBuffer buffer) throws RuntimeIOException {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);

		I2CWrite request = new I2CWrite(controller, address, data, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C write: " + response.getDetail());
		}
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		I2CReadByteData request = new I2CReadByteData(controller, address, register, UUID.randomUUID().toString());

		I2CReadByteResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C read: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		I2CWriteByteData request = new I2CWriteByteData(controller, address, register, b, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeByte: " + response.getDetail());
		}
	}

	@Override
	public void readI2CBlockData(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		I2CReadI2CBlockData request = new I2CReadI2CBlockData(controller, address, register, buffer.remaining(),
				UUID.randomUUID().toString());

		I2CReadResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C read: " + response.getDetail());
		}

		buffer.put(response.getData());
		buffer.flip();
	}

	@Override
	public void writeI2CBlockData(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);

		I2CWriteI2CBlockData request = new I2CWriteI2CBlockData(controller, address, register, data,
				UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C write: " + response.getDetail());
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		I2CClose request = new I2CClose(controller, address, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			Logger.error("Error closing device: " + response.getDetail());
		}
	}
}
