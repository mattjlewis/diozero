package com.diozero.internal.provider.pi4j;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - pi4j provider
 * Filename:     Pi4jI2CDevice.java  
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

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class Pi4jI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private int controller;
	private int address;
	private I2CDevice device;
	
	public Pi4jI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
			int addressSize, int clockFrequency) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.address = address;
		Logger.debug(String.format("Opening I2C device ({}, 0x{})...",
				Integer.valueOf(controller), Integer.toHexString(address)));
		try {
			device = I2CFactory.getInstance(controller).getDevice(address);
		} catch (UnsupportedBusNumberException | IOException e) {
			throw new RuntimeIOException(e);
		}
		Logger.debug(String.format("I2C device ({}, 0x{}) opened",
				Integer.valueOf(controller), Integer.toHexString(address)));
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		// No way to close a Pi4J I2C Device?!
		//i2cDevice.close();
		device = null;
	}

	@Override
	public boolean isOpen() {
		// No way to tell if it is open?!
		return device != null;
	}

	@Override
	public byte readByte() throws RuntimeException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		try {
			int read = device.read();
			if (read < 0) {
				throw new RuntimeIOException("Error reading from I2C device: " + read);
			}
			
			return (byte) read;
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void writeByte(byte b) throws RuntimeException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		try {
			device.write(b);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void read(ByteBuffer dst) throws RuntimeException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		try {
			int read = device.read(buffer, 0, to_read);
			if (read != to_read) {
				throw new RuntimeIOException(
						"Didn't read correct number of bytes, read " + read + ", expected " + to_read);
			}
			dst.put(buffer);
			dst.flip();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void write(ByteBuffer src) throws RuntimeException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		try {
			device.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		try {
			int read = device.read(register);
			if (read < 0) {
				throw new RuntimeIOException("Error reading from I2C device: " + read);
			}
			
			return (byte) read;
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		try {
			device.write(register, b);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void readI2CBlockData(int register, int subAddressSize, ByteBuffer dst) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		try {
			int read = device.read(register, buffer, 0, to_read);
			if (read != to_read) {
				throw new RuntimeIOException("Didn't read correct number of bytes, read " + read + ", expected " + to_read);
			}
			dst.put(buffer);
			dst.flip();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void writeI2CBlockData(int register, int subAddressSize, ByteBuffer src) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		try {
			device.write(register, buffer, 0, to_write);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
