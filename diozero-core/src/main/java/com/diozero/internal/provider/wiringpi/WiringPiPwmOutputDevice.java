package com.diozero.internal.provider.wiringpi;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.PwmType;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;
import com.pi4j.wiringpi.SoftPwm;

public class WiringPiPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(WiringPiPwmOutputDevice.class);
	
	private static final int HARDWARE_PWM_RANGE = 1024;
	// See https://projects.drogon.net/raspberry-pi/wiringpi/software-pwm-library/
	// You can lower the range to get a higher frequency, at the expense of resolution,
	// or increase to get more resolution, but that will lower the frequency
	private static final int SOFTWARE_PWM_RANGE = 100;
	
	private int pinNumber;
	private float value;
	private PwmType pwmType;
	
	WiringPiPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, float initialValue, PwmType pwmType) throws IOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		this.value = initialValue;
		this.pwmType = pwmType;
		
		switch (pwmType) {
		case HARDWARE:
			if (GpioUtil.isExported(pinNumber)) {
				GpioUtil.setDirection(pinNumber, GpioUtil.DIRECTION_OUT);
			} else {
				GpioUtil.export(pinNumber, GpioUtil.DIRECTION_OUT);
			}
			Gpio.pinMode(pinNumber, Gpio.PWM_OUTPUT);
			break;
		case SOFTWARE:
		default:
			int status = SoftPwm.softPwmCreate(pinNumber, 0, SOFTWARE_PWM_RANGE);
			if (status != 0) {
				throw new IOException("Error setting up software controlled PWM GPIO on BCM pin " +
						pinNumber + ", status=" + status);
			}
		}
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		switch (pwmType) {
		case HARDWARE:
			Gpio.pwmWrite(pinNumber, 0);
			GpioUtil.unexport(pinNumber);
		case SOFTWARE:
			SoftPwm.softPwmWrite(pinNumber, 0);
			// TODO No software PWM cleanup method?!
			GpioUtil.unexport(pinNumber);
			break;
		default:
		}
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public float getValue() {
		return value;
	}

	@Override
	public void setValue(float value) throws IOException {
		this.value = value;
		switch (pwmType) {
		case HARDWARE:
			Gpio.pwmWrite(pinNumber, (int)(value * HARDWARE_PWM_RANGE));
			break;
		case SOFTWARE:
		default:
			SoftPwm.softPwmWrite(pinNumber, (int)(value * SOFTWARE_PWM_RANGE));
			break;
		}
	}
}
