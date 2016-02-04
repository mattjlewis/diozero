package com.diozero.internal.provider.pi4j;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.pi4j.io.gpio.*;

public class Pi4jGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(Pi4jGpioOutputDevice.class);

	private GpioPinDigitalOutput digitalOutputPin;
	private int pinNumber;

	Pi4jGpioOutputDevice(String key, DeviceFactoryInterface deviceFactory, GpioController gpioController, int pinNumber, boolean initialValue) {
		super(key, deviceFactory);
		
		Pin pin = RaspiGpioBcm.getPin(pinNumber);
		if (pin == null) {
			throw new IllegalArgumentException("Illegal pin number: " + pinNumber);
		}
		
		this.pinNumber = pinNumber;
		
		digitalOutputPin = gpioController.provisionDigitalOutputPin(pin, "Digital output for BCM GPIO " + pinNumber,
				PinState.getState(initialValue));
	}

	@Override
	public void closeDevice() {
		logger.debug("closeDevice()");
		digitalOutputPin.setState(false);
		digitalOutputPin.unexport();
	}

	@Override
	public boolean getValue() throws IOException {
		return digitalOutputPin.getState().isHigh();
	}

	@Override
	public void setValue(boolean value) throws IOException {
		digitalOutputPin.setState(value);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
}
