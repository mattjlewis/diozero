package com.diozero.internal.provider.wiringpi;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(WiringPiDigitalOutputDevice.class);
	
	private int pinNumber;
	
	WiringPiDigitalOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, boolean initialValue) throws IOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		try {
			if (GpioUtil.isExported(pinNumber)) {
				GpioUtil.setDirection(pinNumber, initialValue ? GpioUtil.DIRECTION_HIGH : GpioUtil.DIRECTION_LOW);
			} else {
				GpioUtil.export(pinNumber, initialValue ? GpioUtil.DIRECTION_HIGH : GpioUtil.DIRECTION_LOW);
			}
			Gpio.pinMode(pinNumber, Gpio.OUTPUT);
		} catch (RuntimeException re) {
			throw new IOException(re);
		}
	}

	@Override
	public void closeDevice() {
		logger.debug("closeDevice()");
		GpioUtil.unexport(pinNumber);
	}

	@Override
	public boolean getValue() throws IOException {
		return Gpio.digitalRead(pinNumber) == 1;
	}

	@Override
	public void setValue(boolean value) throws IOException {
		Gpio.digitalWrite(pinNumber, value);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
}
