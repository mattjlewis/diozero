package com.diozero.internal.provider.wiringpi;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class WiringPiI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final Logger logger = LogManager.getLogger(WiringPiI2CDevice.class);
	
	// TODO Switch to the wiringPi com.pi4j.wiringpi.I2C class?
	private I2CDevice i2cDevice;
	private int controller;
	private int address;
	private boolean open;
	
	public WiringPiI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
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
