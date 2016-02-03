package com.diozero.internal.provider.test;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

public class TestDigitalOutputPin extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(TestDigitalOutputPin.class);
	
	private int pinNumber;
	private boolean value;

	public TestDigitalOutputPin(String key, DeviceFactoryInterface deviceFactory, int pinNumber, boolean initialValue) {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
	}

	@Override
	public boolean getValue() throws IOException {
		return value;
	}

	@Override
	public void setValue(boolean value) throws IOException {
		logger.debug("setValue(" + value + ")");
		this.value = value;
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
}
