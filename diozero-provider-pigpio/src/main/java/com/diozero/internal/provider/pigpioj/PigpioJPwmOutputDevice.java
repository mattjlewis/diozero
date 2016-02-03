package com.diozero.internal.provider.pigpioj;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;

public class PigpioJPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final Logger logger = Logger.getLogger(PigpioJPwmOutputDevice.class.getName());
	
	private int pinNumber;

	public PigpioJPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			float initialValue) {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
	}

	@Override
	public void closeDevice() {
		try {
			setValue(0);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Error setting value to 0 in closeDevice()", e);
		}
		// Nothing more to do?
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public float getValue() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setValue(float value) throws IOException {
		// TODO Auto-generated method stub
	}
}
