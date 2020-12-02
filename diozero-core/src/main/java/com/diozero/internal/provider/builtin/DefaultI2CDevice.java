package com.diozero.internal.provider.builtin;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     DefaultI2CDevice.java  
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

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.I2CSMBusInterface;
import com.diozero.internal.provider.builtin.i2c.NativeI2CDeviceSMBus;
import com.diozero.internal.provider.builtin.i2c.NativeI2CDeviceSysFs;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.util.LibraryLoader;
import com.diozero.util.PropertyUtil;

public class DefaultI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static boolean USE_SYSFS = false;
	private static boolean I2C_SLAVE_FORCE = false;
	static {
		LibraryLoader.loadSystemUtils();

		USE_SYSFS = PropertyUtil.isPropertySet("I2C_USE_SYSFS");
		I2C_SLAVE_FORCE = PropertyUtil.isPropertySet("I2C_SLAVE_FORCE");
	}

	private I2CSMBusInterface i2cDevice;

	public DefaultI2CDevice(DeviceFactoryInterface deviceFactory, String key, int controller, int address,
			I2CConstants.AddressSize addressSize) {
		super(key, deviceFactory);

		boolean force = I2C_SLAVE_FORCE;

		if (USE_SYSFS) {
			Logger.warn("Using sysfs for I2C communication");
			i2cDevice = new NativeI2CDeviceSysFs(controller, address, force);
		} else {
			i2cDevice = new NativeI2CDeviceSMBus(controller, address, addressSize, force);
		}
	}

	@Override
	public boolean probe(I2CDevice.ProbeMode mode) {
		return i2cDevice.probe(mode);
	}

	@Override
	public void writeQuick(byte bit) {
		i2cDevice.writeQuick(bit);
	}

	@Override
	public byte readByte() {
		return i2cDevice.readByte();
	}

	@Override
	public void writeByte(byte b) {
		i2cDevice.writeByte(b);
	}

	@Override
	public byte readByteData(int register) {
		return i2cDevice.readByteData(register);
	}

	@Override
	public void writeByteData(int register, byte b) {
		i2cDevice.writeByteData(register, b);
	}
	
	@Override
	public short readWordData(int register) {
		return i2cDevice.readWordData(register);
	}
	
	@Override
	public void writeWordData(int register, short data) {
		i2cDevice.writeWordData(register, data);
	}

	@Override
	public short processCall(int register, short data) {
		return i2cDevice.processCall(register, data);
	}
	
	@Override
	public byte[] readBlockData(int register) {
		return i2cDevice.readBlockData(register);
	}
	
	@Override
	public void writeBlockData(int register, byte... buffer) {
		i2cDevice.writeBlockData(register, buffer);
	}
	
	@Override
	public byte[] blockProcessCall(int register, byte... data) {
		return i2cDevice.blockProcessCall(register, data);
	}

	@Override
	public int readI2CBlockData(int register, byte[] buffer) {
		return i2cDevice.readI2CBlockData(register, buffer);
	}

	@Override
	public void writeI2CBlockData(int register, byte... data) {
		i2cDevice.writeI2CBlockData(register, data);
	}

	@Override
	public int readBytes(byte[] buffer) {
		return i2cDevice.readBytes(buffer);
	}

	@Override
	public void writeBytes(byte... data) {
		i2cDevice.writeBytes(data);
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice()");
		i2cDevice.close();
	}
}
