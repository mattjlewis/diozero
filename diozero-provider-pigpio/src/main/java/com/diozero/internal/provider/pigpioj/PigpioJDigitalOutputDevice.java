package com.diozero.internal.provider.pigpioj;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.pigpioj.PigpioGpio;
import com.diozero.util.RuntimeIOException;

public class PigpioJDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private int pinNumber;

	public PigpioJDigitalOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			boolean initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		
		int rc = PigpioGpio.setMode(pinNumber, PigpioGpio.MODE_PI_OUTPUT);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.setMode(), respone: " + rc);
		}
		setValue(initialValue);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = PigpioGpio.read(pinNumber);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.read(), respone: " + rc);
		}
		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		int rc = PigpioGpio.write(pinNumber, value);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.write(), respone: " + rc);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		// No GPIO close method in pigpio
	}
}
