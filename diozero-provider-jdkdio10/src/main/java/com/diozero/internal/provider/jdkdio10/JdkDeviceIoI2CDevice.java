package com.diozero.internal.provider.jdkdio10;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - JDK Device I/O v1.0 provider
 * Filename:     JdkDeviceIoI2CDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import java.io.IOException;
import java.nio.ByteBuffer;

import org.pmw.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class JdkDeviceIoI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private I2CDeviceConfig deviceConfig;
	private I2CDevice device;

	public JdkDeviceIoI2CDevice(String key, DeviceFactoryInterface deviceFactory,
			int controllerNumber, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.deviceConfig = new I2CDeviceConfig(controllerNumber, address, addressSize, clockFrequency);
		try {
			device = DeviceManager.open(deviceConfig);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public boolean isOpen() {
		return device.isOpen();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		if (device.isOpen()) {
			try {
				device.close();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
	
	@Override
	public boolean probe(com.diozero.api.I2CDevice.ProbeMode mode) {
		try {
			return device.read() >= 0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}

		byte b;
		try {
			int read = device.read();
			if (read < 0) {
				throw new RuntimeIOException("Error reading from I2C device: " + read);
			}
			b = (byte) read;
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
		return b;
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		try {
			device.write(b);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void read(ByteBuffer dst) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int to_read = dst.remaining();
		try {
			int read = device.read(dst);
			if (read != to_read) {
				throw new RuntimeIOException(
						"Didn't read correct number of bytes, read " + read + ", expected " + to_read);
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void write(ByteBuffer src) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int to_write = src.remaining();
		try {
			int written = device.write(src);
			if (written != to_write) {
				throw new RuntimeIOException(
						"Didn't write correct number of bytes, wrote " + written + ", expected " + to_write);
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public byte readByteData(int address) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		byte b;
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(1);
			int read = device.read(address, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, buffer);
			if (read < 0) {
				throw new RuntimeIOException("Error reading from I2C device: " + read);
			}
			b = buffer.get(0);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
		return b;
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(1);
			buffer.put(b);
			int written = device.write(register, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, buffer);
			if (written != 1) {
				throw new RuntimeIOException(
						"Didn't write correct number of bytes, wrote " + written + ", expected 1");
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void readI2CBlockData(int address, int subAddressSize, ByteBuffer dst) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int to_read = dst.remaining();
		try {
			int read = device.read(address, subAddressSize, dst);
			if (read != to_read) {
				throw new RuntimeIOException(
						"Didn't read correct number of bytes, read " + read + ", expected " + to_read);
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void writeI2CBlockData(int register, int subAddressSize, ByteBuffer src) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int to_write = src.remaining();
		try {
			int written = device.write(register, subAddressSize, src);
			if (written != to_write) {
				throw new RuntimeIOException(
						"Didn't write correct number of bytes, wrote " + written + ", expected " + to_write);
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
