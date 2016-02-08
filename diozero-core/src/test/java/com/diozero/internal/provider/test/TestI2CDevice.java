package com.diozero.internal.provider.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.I2CDeviceInterface;

public class TestI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	private static final Logger logger = LogManager.getLogger(TestI2CDevice.class);

	public TestI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int address, int addressSize, int clockFrequency) {
		super(key, deviceFactory);
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer buffer) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer buffer) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	protected void closeDevice() throws IOException {
		logger.debug("closeDevice()");
	}
}
