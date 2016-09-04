package com.diozero.internal.provider.sysfs;

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
