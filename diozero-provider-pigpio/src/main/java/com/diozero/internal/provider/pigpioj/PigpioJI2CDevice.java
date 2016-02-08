package com.diozero.internal.provider.pigpioj;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.pigpioj.PigpioI2C;

public class PigpioJI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final Logger logger = LogManager.getLogger(PigpioJI2CDevice.class);
	
	private static final int CLOSED = -1;
	
	private int controller;
	private int address;
	private int handle = CLOSED;

	public PigpioJI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
			int addressSize) throws IOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.address = address;
		
		handle = PigpioI2C.i2cOpen(controller, address, 0);
		logger.debug(String.format("I2C device (%d, 0x%x) opened, handle=%d",
				Integer.valueOf(controller), Integer.valueOf(address), Integer.valueOf(handle)));
		if (handle < 0) {
			handle = CLOSED;
			throw new IOException(String.format("Error opening I2C device on bus %d, address 0x%x",
					Integer.valueOf(controller), Integer.valueOf(address)));
		}
	}

	@Override
	public boolean isOpen() {
		return handle >= 0;
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer dst) throws IOException {
		if (! isOpen()) {
			throw new IllegalStateException("I2C Device " + controller + "-" + address + " is closed");
		}
		
		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		logger.debug("reading " + to_read + " bytes");
		int read = PigpioI2C.i2cReadI2CBlockData(handle, register, buffer, to_read);
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
		PigpioI2C.i2cWriteI2CBlockData(handle, register, buffer, to_write);
	}

	@Override
	protected void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		PigpioI2C.i2cClose(handle);
		handle = CLOSED;
	}
}
