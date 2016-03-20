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
	private static final boolean DISABLED = true;
	
	private int pinNumber;
	private float value;
	private PwmType pwmType;
	private int range;
	
	WiringPiPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, PwmType pwmType,
			int range, int pinNumber, float initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		this.value = initialValue;
		this.pwmType = pwmType;
		this.range = range;
		
		switch (pwmType) {
		case HARDWARE:
			if (DISABLED) {
				// This worked from the command line after a fresh restart
				// However, not yet got it to work in software even though using the same commands
				// http://raspberrypi.stackexchange.com/questions/4906/control-hardware-pwm-frequency/38070#38070?newreg=67e978faf30840a6a674ba040fbf1752
				throw new UnsupportedOperationException("Not yet worked out WiringPi Hardware PWM");
			}
			if (GpioUtil.isExported(pinNumber)) {
				GpioUtil.setDirection(pinNumber, GpioUtil.DIRECTION_OUT);
			} else {
				GpioUtil.export(pinNumber, GpioUtil.DIRECTION_OUT);
			}
			Gpio.pinMode(pinNumber, Gpio.PWM_OUTPUT);
			Gpio.pwmWrite(pinNumber, (int)(value * range));
			break;
		case SOFTWARE:
			int status = SoftPwm.softPwmCreate(pinNumber, (int)(value * range), range);
			if (status != 0) {
				throw new RuntimeIOException("Error setting up software controlled PWM GPIO on BCM pin " +
						pinNumber + ", status=" + status);
			}
			break;
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		switch (pwmType) {
		case HARDWARE:
			GpioUtil.unexport(pinNumber);
		case SOFTWARE:
			SoftPwm.softPwmStop(pinNumber);
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
		int dc = (int)Math.floor(value * range);
		switch (pwmType) {
		case HARDWARE:
			Logger.info("setValue({}), range={}, dc={}", Float.valueOf(value), Integer.valueOf(range), Integer.valueOf(dc));
			Gpio.pwmWrite(pinNumber, dc);
			break;
		case SOFTWARE:
		default:
			SoftPwm.softPwmWrite(pinNumber, dc);
			break;
		}
	}
}
