package com.diozero.internal.provider.jdkdio10;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

public class JdkDeviceIoGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(JdkDeviceIoGpioOutputDevice.class);
	
	private GPIOPinConfig pinConfig;
	private GPIOPin pin;
	
	JdkDeviceIoGpioOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, boolean initialValue) throws IOException {
		super(key, deviceFactory);
		
		pinConfig = new GPIOPinConfig(DeviceConfig.DEFAULT, pinNumber, GPIOPinConfig.DIR_OUTPUT_ONLY,
				GPIOPinConfig.DEFAULT, GPIOPinConfig.TRIGGER_NONE, initialValue);
				//GPIOPinConfig.MODE_OUTPUT_PUSH_PULL, GPIOPinConfig.TRIGGER_NONE, false);
		pin = DeviceManager.open(GPIOPin.class, pinConfig);
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		if (pin.isOpen()) {
			pin.close();
		}
	}
	
	// Exposed properties
	@Override
	public int getPin() {
		return pinConfig.getPinNumber();
	}
	
	@Override
	public boolean getValue() throws IOException {
		return pin.getValue();
	}
	
	@Override
	public void setValue(boolean value) throws IOException {
		pin.setValue(value);
	}
}
