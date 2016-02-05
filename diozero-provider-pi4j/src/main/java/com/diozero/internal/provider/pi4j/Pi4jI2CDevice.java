package com.diozero.internal.provider.pi4j;

/*
 * #%L
 * Device I/O Zero - pi4j provider
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

public class Pi4jI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final Logger logger = LogManager.getLogger(Pi4jI2CDevice.class);

	private I2CDevice i2cDevice;
	private int controller;
	private int address;
	private boolean open;
	
	public Pi4jI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
			int addressSize, int clockFrequency) throws IOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.address = address;
		i2cDevice = I2CFactory.getInstance(controller).getDevice(address);
		open = true;
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		// No way to close a Pi4J I2C Device?!
		//i2cDevice.close();
		open = false;
	}

	@Override
	public boolean isOpen() {
		// No way to tell if it is open?!
		return open;
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer dst) throws IOException {
		if (! open) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		int read = i2cDevice.read(register, buffer, 0, to_read);
		if (read != to_read) {
			throw new IOException("Didn't read correct number of bytes, read " + read + ", expected " + to_read);
		}
		dst.put(buffer);
		dst.flip();
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer src) throws IOException {
		if (! open) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		i2cDevice.write(register, buffer, 0, to_write);
	}
}
