package com.diozero.internal.provider.jdkdio10;

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
	public JdkDeviceIoI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controllerNumber, int address, int addressSize, int clockFrequency) throws IOException {
		super(key, deviceFactory);
		
		this.deviceConfig = new I2CDeviceConfig(controllerNumber, address, addressSize, clockFrequency);
		device = DeviceManager.open(deviceConfig);
	}
	
	@Override
	public boolean isOpen() {
		return device.isOpen();
	}

	@Override
	public void closeDevice() {
		logger.debug("closeDevice()");
		if (device.isOpen()) {
			try { device.close(); } catch (Exception e) { }
		}
	}

	@Override
	public void read(int address, int subAddressSize, ByteBuffer buffer) throws IOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		int result = device.read(address, subAddressSize, buffer);
		if (result != buffer.capacity()) {
			throw new IOException(
					"Didn't read correct number of bytes, read " + result + ", expected " + buffer.capacity());
		}
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer buffer) throws IOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("I2C Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		device.write(register, subAddressSize, buffer);
	}
}
