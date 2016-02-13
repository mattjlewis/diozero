package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Device I/O Zero - wiringPi provider
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.pmw.tinylog.Logger;

import com.diozero.api.PwmType;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;
import com.pi4j.wiringpi.SoftPwm;

public class WiringPiPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final int HARDWARE_PWM_RANGE = 1024;
	// See https://projects.drogon.net/raspberry-pi/wiringpi/software-pwm-library/
	// You can lower the range to get a higher frequency, at the expense of resolution,
	// or increase to get more resolution, but that will lower the frequency
	private static final int SOFTWARE_PWM_RANGE = 100;
	
	private int pinNumber;
	private float value;
	private PwmType pwmType;
	
	WiringPiPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, float initialValue, PwmType pwmType) throws RuntimeIOException {
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
				throw new RuntimeIOException("Error setting up software controlled PWM GPIO on BCM pin " +
						pinNumber + ", status=" + status);
			}
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
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
	public void setValue(float value) throws RuntimeIOException {
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
