package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     SysFsI2CDevice.java  
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


import java.nio.ByteBuffer;

import org.pmw.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.LibraryLoader;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RuntimeIOException;

public class SysFsI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static boolean USE_SYSFS = false;
	private static boolean I2C_SLAVE_FORCE = false;
	static {
		LibraryLoader.loadLibrary(SysFsI2CDevice.class, "diozero-system-utils");
		
		USE_SYSFS = PropertyUtil.isPropertySet("I2C_USE_SYSFS");
		I2C_SLAVE_FORCE = PropertyUtil.isPropertySet("I2C_SLAVE_FORCE");
	}
	
	private I2CSMBusInterface i2cDevice;
	
	public SysFsI2CDevice(DeviceFactoryInterface deviceFactory, String key, int controller,
			int address, int addressSize, int frequency) {
		super(key, deviceFactory);
		
		boolean force = I2C_SLAVE_FORCE;

		if (USE_SYSFS) {
			Logger.warn("Using sysfs for I2C communication");
			i2cDevice = new NativeI2CDeviceSysFs(controller, address, force);
		} else {
			i2cDevice = new NativeI2CDeviceSMBus(controller, address, force);
		}
	}
	
	@Override
	public boolean probe(I2CDevice.ProbeMode mode) {
		return i2cDevice.probe(mode);
	}
	
	@Override
	public byte readByte() throws RuntimeException {
		return i2cDevice.readByte();
	}
	
	@Override
	public void writeByte(byte b) throws RuntimeException {
		i2cDevice.writeByte(b);
	}

	@Override
	public void read(ByteBuffer dst) throws RuntimeException {
		byte[] buffer = i2cDevice.readBytes(dst.remaining());
		dst.put(buffer);
		dst.flip();
	}

	@Override
	public void write(ByteBuffer src) throws RuntimeException {
		byte[] buffer = new byte[src.remaining()];
		src.get(buffer);
		i2cDevice.writeBytes(buffer);
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		return i2cDevice.readByteData(register);
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		i2cDevice.writeByteData(register, b);
	}

	@Override
	public void readI2CBlockData(int register, int subAddressSize, ByteBuffer dst) throws RuntimeIOException {
		dst.put(i2cDevice.readI2CBlockData(register, dst.remaining()));
		dst.flip();
	}

	@Override
	public void writeI2CBlockData(int register, int subAddressSize, ByteBuffer src) throws RuntimeIOException {
		byte[] buffer = new byte[src.remaining()];
		src.get(buffer);
		i2cDevice.writeI2CBlockData(register, buffer);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		i2cDevice.close();
	}
}
