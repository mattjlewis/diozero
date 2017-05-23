package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Java Sysfs provider
 * %%
 * Copyright (C) 2016 mattjlewis
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

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.sysfs.I2CSMBusInterface.NotSupportedException;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

public class SysFsI2cDevice extends AbstractDevice implements I2CDeviceInterface {
	private static boolean USE_SYSFS = false;
	static {
		LibraryLoader.loadLibrary(SysFsI2cDevice.class, "diozero-system-utils");
		
		String use_sysfs = System.getProperty("I2C_USE_SYSFS");
		if (use_sysfs != null) {
			if (use_sysfs.trim().equals("")) {
				USE_SYSFS = true;
			} else {
				USE_SYSFS = Boolean.parseBoolean(use_sysfs);
			}
		}
	}
	
	private I2CSMBusInterface i2cDevice;
	
	public SysFsI2cDevice(DeviceFactoryInterface deviceFactory, String key, int controller,
			int address, int addressSize, int frequency) {
		super(key, deviceFactory);
		
		boolean force = false;

		if (USE_SYSFS) {
			Logger.warn("Using sysfs for I2C communication");
			i2cDevice = new NativeI2CDeviceSysFs(controller, address, force);
		} else {
			try {
				i2cDevice = new NativeI2CDeviceSMBus(controller, address, force);
			} catch (NotSupportedException e) {
				Logger.warn(e, "Error initialising I2C SMBus for controller {}, device 0x{0x}: {}", Integer.valueOf(controller), Integer.valueOf(address), e);
				Logger.warn("Using sysfs for I2C communication");
				i2cDevice = new NativeI2CDeviceSysFs(controller, address, force);
			}
		}
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
		i2cDevice.close();
	}
}
