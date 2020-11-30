package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Provider
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

import java.util.UUID;

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.I2CDevice.ProbeMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.remote.message.I2CBlockProcessCall;
import com.diozero.remote.message.I2CBooleanResponse;
import com.diozero.remote.message.I2CByteResponse;
import com.diozero.remote.message.I2CBytesResponse;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CProbe;
import com.diozero.remote.message.I2CProcessCall;
import com.diozero.remote.message.I2CReadBlockData;
import com.diozero.remote.message.I2CReadBlockDataResponse;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadBytes;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadWordData;
import com.diozero.remote.message.I2CWordResponse;
import com.diozero.remote.message.I2CWriteBlockData;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteBytes;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.I2CWriteQuick;
import com.diozero.remote.message.I2CWriteWordData;
import com.diozero.remote.message.RemoteProtocolInterface;
import com.diozero.remote.message.Response;

public class RemoteI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private RemoteProtocolInterface remoteProtocol;
	private int controller;
	private int address;

	public RemoteI2CDevice(RemoteDeviceFactory deviceFactory, String key, int controller, int address,
			I2CConstants.AddressSize addressSize) {
		super(key, deviceFactory);

		this.remoteProtocol = deviceFactory.getProtocolHandler();
		this.controller = controller;
		this.address = address;

		I2COpen request = new I2COpen(controller, address, addressSize.getSize(), UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error: " + response.getDetail());
		}
	}

	@Override
	public boolean probe(ProbeMode mode) throws RuntimeIOException {
		I2CProbe request = new I2CProbe(controller, address, mode, UUID.randomUUID().toString());

		I2CBooleanResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C probe: " + response.getDetail());
		}

		return response.getResult();
	}

	@Override
	public void writeQuick(byte bit) {
		I2CWriteQuick request = new I2CWriteQuick(controller, address, bit, UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeQuick: " + response.getDetail());
		}
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		I2CReadByte request = new I2CReadByte(controller, address, UUID.randomUUID().toString());

		I2CByteResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readByte: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		I2CWriteByte request = new I2CWriteByte(controller, address, b, UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeByte: " + response.getDetail());
		}
	}

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		I2CReadBytes request = new I2CReadBytes(controller, address, buffer.length, UUID.randomUUID().toString());

		I2CBytesResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readBytes: " + response.getDetail());
		}

		byte[] response_data = response.getData();
		System.arraycopy(response_data, 0, buffer, 0, response_data.length);

		return response_data.length;
	}

	@Override
	public void writeBytes(byte... data) throws RuntimeIOException {
		I2CWriteBytes request = new I2CWriteBytes(controller, address, data, UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeBytes: " + response.getDetail());
		}
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		I2CReadByteData request = new I2CReadByteData(controller, address, register, UUID.randomUUID().toString());

		I2CByteResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readByteData: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		I2CWriteByteData request = new I2CWriteByteData(controller, address, register, b, UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeByteData: " + response.getDetail());
		}
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		I2CReadWordData request = new I2CReadWordData(controller, address, register, UUID.randomUUID().toString());

		I2CWordResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readWordData: " + response.getDetail());
		}

		return (short) response.getData();
	}

	@Override
	public void writeWordData(int register, short s) throws RuntimeIOException {
		I2CWriteWordData request = new I2CWriteWordData(controller, address, register, s, UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeWordData: " + response.getDetail());
		}
	}

	@Override
	public short processCall(int register, short s) throws RuntimeIOException {
		I2CProcessCall request = new I2CProcessCall(controller, address, register, s, UUID.randomUUID().toString());

		I2CWordResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C processCall: " + response.getDetail());
		}

		return (short) response.getData();
	}

	@Override
	public byte[] readBlockData(int register) throws RuntimeIOException {
		I2CReadBlockData request = new I2CReadBlockData(controller, address, register, UUID.randomUUID().toString());

		I2CReadBlockDataResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readBlockData: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public void writeBlockData(int register, byte... data) throws RuntimeIOException {
		I2CWriteBlockData request = new I2CWriteBlockData(controller, address, register, data,
				UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeBlockData: " + response.getDetail());
		}
	}

	@Override
	public byte[] blockProcessCall(int register, byte... txData) throws RuntimeIOException {
		I2CBlockProcessCall request = new I2CBlockProcessCall(controller, address, register, txData,
				UUID.randomUUID().toString());

		I2CBytesResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C blockProcessCall: " + response.getDetail());
		}

		return response.getData();
	}

	@Override
	public int readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		I2CReadI2CBlockData request = new I2CReadI2CBlockData(controller, address, register, buffer.length,
				UUID.randomUUID().toString());

		I2CBytesResponse response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C readI2CBlockData: " + response.getDetail());
		}

		System.arraycopy(response.getData(), 0, buffer, 0, response.getData().length);
		
		return response.getData().length;
	}

	@Override
	public void writeI2CBlockData(int register, byte... data) throws RuntimeIOException {
		I2CWriteI2CBlockData request = new I2CWriteI2CBlockData(controller, address, register, data,
				UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in I2C writeI2CBlockData: " + response.getDetail());
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		I2CClose request = new I2CClose(controller, address, UUID.randomUUID().toString());

		Response response = remoteProtocol.request(request);
		if (response.getStatus() != Response.Status.OK) {
			Logger.error("Error closing device: " + response.getDetail());
		}
	}
}
