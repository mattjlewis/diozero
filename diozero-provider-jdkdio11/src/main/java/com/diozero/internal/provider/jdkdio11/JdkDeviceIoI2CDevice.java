package com.diozero.internal.provider.jdkdio11;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
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

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class JdkDeviceIoI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final Logger logger = LogManager.getLogger(JdkDeviceIoI2CDevice.class);

	private I2CDeviceConfig deviceConfig;
	private I2CDevice device;

	public JdkDeviceIoI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address, int addressSize, int clockFrequency) throws IOException {
		super(key, deviceFactory);
		
		deviceConfig = new I2CDeviceConfig.Builder().setControllerNumber(controller)
				.setAddress(address, addressSize).setClockFrequency(clockFrequency).build();
		device = DeviceManager.open(deviceConfig);
	}
	
	@Override
	public boolean isOpen() {
		return device.isOpen();
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		if (device.isOpen()) {
			device.close();
		}
	}

	@Override
	public void read(int address, int subAddressSize, ByteBuffer buffer) throws IOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int to_read = buffer.remaining();
		int read = device.read(address, subAddressSize, buffer);
		if (read != to_read) {
			throw new IOException(
					"Didn't read correct number of bytes, read " + read + ", expected " + to_read);
		}
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer src) throws IOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int to_write = src.remaining();
		int written = device.write(register, subAddressSize, src);
		if (written != to_write) {
			throw new IOException(
					"Didn't write correct number of bytes, wrote " + written + ", expected " + to_write);
		}
	}
}
