package com.diozero.internal.provider.pigpioj;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - pigpioj provider
 * Filename:     PigpioJI2CDevice.java  
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

import org.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

import uk.pigpioj.PigpioInterface;

public class PigpioJI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final int CLOSED = -1;
	private static final int I2C_SMBUS_WRITE = 0;

	private PigpioInterface pigpioImpl;
	private int controller;
	private int address;
	private int handle = CLOSED;

	public PigpioJI2CDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl,
			int controller, int address, int addressSize) throws RuntimeIOException {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.controller = controller;
		this.address = address;

		int rc = pigpioImpl.i2cOpen(controller, address, 0);
		if (rc < 0) {
			handle = CLOSED;
			throw new RuntimeIOException(String.format("Error opening I2C device on bus %d, address 0x%x, response: %d",
					Integer.valueOf(controller), Integer.valueOf(address), Integer.valueOf(rc)));
		}
		handle = rc;
		Logger.trace("I2C device ({}, 0x{}) opened, handle={}", Integer.valueOf(controller),
				Integer.toHexString(address), Integer.valueOf(handle));
	}

	@Override
	public boolean isOpen() {
		return handle >= 0;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		int rc = pigpioImpl.i2cClose(handle);
		handle = CLOSED;
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cClose(), response: " + rc);
		}
	}

	@Override
	public boolean probe(I2CDevice.ProbeMode mode) {
		int res;
		switch (mode) {
		case QUICK:
			/* This is known to corrupt the Atmel AT24RF08 EEPROM */
			res = pigpioImpl.i2cWriteQuick(handle, I2C_SMBUS_WRITE);
			break;
		case READ:
			/*
			 * This is known to lock SMBus on various write-only chips (mainly clock chips)
			 */
			res = pigpioImpl.i2cReadByte(handle);
			break;
		default:
			if ((address >= 0x30 && address <= 0x37) || (address >= 0x50 && address <= 0x5F)) {
				res = pigpioImpl.i2cReadByte(handle);
			} else {
				res = pigpioImpl.i2cWriteQuick(handle, I2C_SMBUS_WRITE);
			}
		}
		return res >= 0;
	}

	@Override
	public void writeQuick(byte bit) {
		throw new UnsupportedOperationException("writeQuick not supported in pigpio");
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int read = pigpioImpl.i2cReadByte(handle);
		if (read < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cReadByte(), response: " + read);
		}

		return (byte) read;
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int rc = pigpioImpl.i2cWriteByte(handle, 0xff & b);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteByte(), response: " + rc);
		}
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
	
		int read = pigpioImpl.i2cReadByteData(handle, register);
		if (read < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cReadByteData(), response: " + read);
		}
	
		return (byte) read;
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
	
		int rc = pigpioImpl.i2cWriteByteData(handle, register, b & 0xFF);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteByteData(), response: " + rc);
		}
	}

	@Override
	public short readWordData(int register) {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
	
		int read = pigpioImpl.i2cReadWordData(handle, register);
		if (read < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cReadWordData(), response: " + read);
		}
	
		return (short) read;
	}

	@Override
	public void writeWordData(int register, short data) {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
	
		int rc = pigpioImpl.i2cWriteWordData(handle, register, data & 0xFFFF);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteWordData(), response: " + rc);
		}
	}
	
	@Override
	public short processCall(int register, short data) {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
	
		int rc = pigpioImpl.i2cProcessCall(handle, register, data & 0xFFFF);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cProcessCall(), response: " + rc);
		}
		
		return (short) rc;
	}
	
	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int read = pigpioImpl.i2cReadDevice(handle, buffer, buffer.length);
		if (read < 0) {
			throw new RuntimeIOException("Didn't read correct number of bytes from i2cReadDevice(), read " + read
					+ ", expected " + buffer.length);
		}
		
		return read;
	}

	@Override
	public void writeBytes(byte[] data) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int rc = pigpioImpl.i2cWriteDevice(handle, data, data.length);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteDevice(), response: " + rc);
		}
	}

	@Override
	public int readBlockData(int register, byte[] buffer) {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int rc = pigpioImpl.i2cReadBlockData(handle, register, buffer);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cReadBlockData(), response: " + rc);
		}
		
		return rc;
	}
	
	@Override
	public void writeBlockData(int register, byte[] data) {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int rc = pigpioImpl.i2cWriteBlockData(handle, register, data, data.length);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteBlockData(), response: " + rc);
		}
	}

	@Override
	public byte[] blockProcessCall(int register, byte[] data) {
		byte[] txrx_data = new byte[data.length];
		System.arraycopy(data, 0, txrx_data, 0, data.length);
		
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int rc = pigpioImpl.i2cBlockProcessCall(handle, register, txrx_data, txrx_data.length);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteBlockData(), response: " + rc);
		}
		
		return txrx_data;
	}

	@Override
	public void readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int read = pigpioImpl.i2cReadI2CBlockData(handle, register, buffer, buffer.length);
		if (read < 0 || read != buffer.length) {
			throw new RuntimeIOException("Didn't read correct number of bytes from i2cReadI2CBlockData(), read " + read
					+ ", expected " + buffer.length);
		}
	}

	@Override
	public void writeI2CBlockData(int register, byte[] data) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}

		int rc = pigpioImpl.i2cWriteI2CBlockData(handle, register, data, data.length);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.i2cWriteI2CBlockData(), response: " + rc);
		}
	}
}
