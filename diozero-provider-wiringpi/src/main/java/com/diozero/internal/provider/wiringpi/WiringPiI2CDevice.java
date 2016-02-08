package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Device I/O Zero - wiringPi provider
 * %%
 * Copyright (C) 2016 diozero
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class WiringPiI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final Logger logger = LogManager.getLogger(WiringPiI2CDevice.class);
	
	//private static final int CLOSED = -1;
	
	private int controller;
	private int address;
	//private int handle = CLOSED;
	private I2CDevice i2cDevice;
	
	public WiringPiI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
			int addressSize, int clockFrequency) throws IOException {
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
		logger.debug(String.format("Opening I2C device (%d, 0x%x)...",
				Integer.valueOf(controller), Integer.valueOf(address)));
		i2cDevice = I2CFactory.getInstance(controller).getDevice(address);
		logger.debug(String.format("I2C device (%d, 0x%x) opened",
				Integer.valueOf(controller), Integer.valueOf(address)));
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		// No way to close a wiringPi I2C Device?!
		//handle = CLOSED;
		// No way to close a Pi4J I2C Device?!
		//i2cDevice.close();
		i2cDevice = null;
	}

	@Override
	public boolean isOpen() {
		// No way to tell if it is open?!
		//return handle != CLOSED;
		return i2cDevice != null;
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer dst) throws IOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		logger.debug("reading " + to_read + " bytes");
		// TODO Need to loop, yuck
		//byte b = I2C.wiringPiI2CReadReg8(handle, register);
		int read = i2cDevice.read(register, buffer, 0, to_read);
		if (read != to_read) {
			throw new IOException("Didn't read correct number of bytes, read " + read + ", expected " + to_read);
		}
		dst.put(buffer);
		dst.flip();
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer src) throws IOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		// Need to loop, yuck
		//I2C.wiringPiI2CWriteReg8(handle, register, b);
		i2cDevice.write(register, buffer, 0, to_write);
	}
}
