package com.diozero.internal.provider.jpi;

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

import com.diozero.internal.provider.i2c.NativeI2CDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class SysFsI2cDevice extends AbstractDevice implements I2CDeviceInterface {
	private NativeI2CDevice i2cDevice;
	
	public SysFsI2cDevice(DeviceFactoryInterface deviceFactory, String key, int controller,
			int address, int addressSize, int frequency) {
		super(key, deviceFactory);

		i2cDevice = new NativeI2CDevice(controller, address);
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
	public void read(int register, int subAddressSize, ByteBuffer dst) throws RuntimeIOException {
		dst.put(i2cDevice.readBlockData(register, dst.remaining()));
		dst.flip();
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer src) throws RuntimeIOException {
		byte[] buffer = new byte[src.remaining()];
		src.get(buffer);
		i2cDevice.writeBlockData(register, buffer);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		i2cDevice.close();
	}
}
