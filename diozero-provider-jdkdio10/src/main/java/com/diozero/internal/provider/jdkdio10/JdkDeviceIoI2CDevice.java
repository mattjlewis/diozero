package com.diozero.internal.provider.jdkdio10;

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

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class JdkDeviceIoI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private I2CDeviceConfig deviceConfig;
	private I2CDevice device;

	/**
	 * @param controllerNumber
	 *            the number of the bus the slave device is connected to (a
	 *            positive or zero integer) or
	 *            {@link jdk.dio.i2cbus.I2CDeviceConfig.DEFAULT}.
	 * @param address
	 *            the address of the slave device on the bus (a positive or zero
	 *            integer).
	 * @param addressSize
	 *            the address size:
	 *            {@link jdk.dio.i2cbus.I2CDeviceConfig.ADDR_SIZE_7} bits,
	 *            {@link jdk.dio.i2cbus.I2CDeviceConfig.ADDR_SIZE_10} bits or
	 *            {@link jdk.dio.i2cbus.I2CDeviceConfig.DEFAULT}.
	 * @param clockFrequency
	 *            the clock frequency of the slave device in Hz (a positive
	 *            integer) or {@link jdk.dio.i2cbus.I2CDeviceConfig.DEFAULT}.
	 */
	public JdkDeviceIoI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controllerNumber, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
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
	public void closeDevice() {
		Logger.debug("closeDevice()");
		if (device.isOpen()) {
			try { device.close(); } catch (Exception e) { }
		}
	}

	@Override
	public void read(int address, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int result;
		try {
			result = device.read(address, subAddressSize, buffer);
			if (result != buffer.capacity()) {
				throw new RuntimeIOException(
						"Didn't read correct number of bytes, read " + result + ", expected " + buffer.capacity());
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		try {
			device.write(register, subAddressSize, buffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
