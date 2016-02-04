package com.diozero.internal.provider.jdkdio11;

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
