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

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class SysFsI2cDevice extends AbstractDevice implements I2CDeviceInterface {
	private int fd;
	
	public SysFsI2cDevice(DeviceFactoryInterface deviceFactory, String key, int controller,
			int address, int addressSize, int frequency) {
		super(key, deviceFactory);
		
		fd = NativeSysFsI2C.open(controller, address);
		if (fd < 0) {
			throw new RuntimeIOException("Error in native I2C open, return=" + fd);
		}
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer dst) throws RuntimeIOException {
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		int read = NativeSysFsI2C.readBlockData(fd, register, buffer, to_read);
		if (read < 0 || read != to_read) {
			throw new RuntimeIOException("Didn't read correct number of bytes, read " + read + ", expected " + to_read);
		}
		dst.put(buffer);
		dst.flip();
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer src) throws RuntimeIOException {
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		int rc = NativeSysFsI2C.writeBlockData(fd, register, buffer, to_write);
		if (rc < 0) {
			throw new RuntimeIOException("Error in NativeSysFsI2C.writeBlockData(), response: " + rc);
		}
	}

	@Override
	public void read(ByteBuffer dst) throws RuntimeException {
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		int read = NativeSysFsI2C.readDevice(fd, buffer, to_read);
		if (read < 0 || read != to_read) {
			throw new RuntimeIOException("Didn't read correct number of bytes, read " + read + ", expected " + to_read);
		}
		dst.put(buffer);
		dst.flip();
	}

	@Override
	public void write(ByteBuffer src) throws RuntimeException {
		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		int rc = NativeSysFsI2C.writeDevice(fd, buffer, to_write);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioI2C.i2cWriteI2CBlockData(), response: " + rc);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		NativeSysFsI2C.close(fd);
	}
}
