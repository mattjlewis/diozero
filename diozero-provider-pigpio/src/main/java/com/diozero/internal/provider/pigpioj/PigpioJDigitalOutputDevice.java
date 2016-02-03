package com.diozero.internal.provider.pigpioj;

import java.io.IOException;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.pigpioj.PigpioGpio;

public class PigpioJDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private int pinNumber;

	public PigpioJDigitalOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			boolean initialValue) throws IOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		
		PigpioGpio.setMode(pinNumber, PigpioGpio.MODE_PI_OUTPUT);
		setValue(initialValue);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws IOException {
		return PigpioGpio.read(pinNumber);
	}

	@Override
	public void setValue(boolean value) throws IOException {
		PigpioGpio.write(pinNumber, value);
	}

	@Override
	protected void closeDevice() throws IOException {
		// No GPIO close method in pigpio
	}
}
