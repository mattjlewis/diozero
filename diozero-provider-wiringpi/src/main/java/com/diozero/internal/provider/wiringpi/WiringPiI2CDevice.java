package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - wiringPi provider
 * Filename:     WiringPiI2CDevice.java  
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


import java.io.IOException;
import java.nio.ByteBuffer;

import org.tinylog.Logger;

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class WiringPiI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	//private static final int CLOSED = -1;
	
	private int controller;
	private int address;
	//private int handle = CLOSED;
	private I2CDevice device;
	
	public WiringPiI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
			int addressSize) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.address = address;
		/*
		handle = I2C.wiringPiI2CSetup(address);
		if (handle == -1) {
			handle = CLOSED;
			throw new IOException("Error in I2C.wiringPiI2CSetup(" + address + ")");
		}
		*/
		Logger.debug("Opening I2C device ({}, 0x{})...",
				Integer.valueOf(controller), Integer.toHexString(address));
		try {
			device = I2CFactory.getInstance(controller).getDevice(address);
		} catch (UnsupportedBusNumberException | IOException e) {
			throw new RuntimeIOException(e);
		}
		Logger.debug("I2C device ({}, 0x{}) opened",
				Integer.valueOf(controller), Integer.toHexString(address));
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		// No way to close a wiringPi I2C Device?!
		//handle = CLOSED;
		// No way to close a Pi4J I2C Device?!
		//i2cDevice.close();
		device = null;
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
	public boolean isOpen() {
		// No way to tell if it is open?!
		//return handle != CLOSED;
		return device != null;
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		try {
			// Need to loop if using wiringPi JNI, yuck
			//I2C.wiringPiI2CWrite(handle, b);
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
	public void writeByte(byte b) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		try {
			// Need to loop if using wiringPi JNI, yuck
			//I2C.wiringPiI2CWrite(handle, b);
			device.write(b);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void read(ByteBuffer dst) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		try {
			// Need to loop if using wiringPi JNI, yuck
			//byte b = I2C.wiringPiI2CRead(handle);
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
	public void write(ByteBuffer src) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		try {
			// Need to loop if using wiringPi JNI, yuck
			//I2C.wiringPiI2CWrite(handle, b);
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
			// Need to loop if using wiringPi JNI, yuck
			//byte b = I2C.wiringPiI2CReadReg8(handle, register);
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
		
		// Need to loop if using wiringPi JNI, yuck
		//I2C.wiringPiI2CWriteReg8(handle, register, b);
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
		// Need to loop if using wiringPi JNI, yuck
		//byte b = I2C.wiringPiI2CReadReg8(handle, register);
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
		// Need to loop if using wiringPi JNI, yuck
		//I2C.wiringPiI2CWriteReg8(handle, register, b);
		try {
			device.write(register, buffer, 0, to_write);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
