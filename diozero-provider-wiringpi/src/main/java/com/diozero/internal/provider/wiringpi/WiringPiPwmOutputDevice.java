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
	private int gpio;
	private float value;
	private PwmType pwmType;
	private int range;
	
	WiringPiPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, PwmType pwmType,
			int range, int gpio, float initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.pwmType = pwmType;
		this.gpio = gpio;
		this.value = initialValue;
		this.range = range;
		
		switch (pwmType) {
		case HARDWARE:
			if (GpioUtil.isExported(gpio)) {
				GpioUtil.setDirection(gpio, GpioUtil.DIRECTION_OUT);
			} else {
				GpioUtil.export(gpio, GpioUtil.DIRECTION_OUT);
			}
			Gpio.pinMode(gpio, Gpio.PWM_OUTPUT);
			// Have to call this after setting the pin mode! Yuck
			Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
			Gpio.pwmWrite(gpio, Math.round(initialValue * range));
			break;
		case SOFTWARE:
			int status = SoftPwm.softPwmCreate(gpio, Math.round(initialValue * range), range);
			if (status != 0) {
				throw new RuntimeIOException("Error setting up software controlled PWM GPIO on BCM pin " +
						gpio + ", status=" + status);
			}
			break;
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		switch (pwmType) {
		case HARDWARE:
			GpioUtil.unexport(gpio);
		case SOFTWARE:
			SoftPwm.softPwmStop(gpio);
			GpioUtil.unexport(gpio);
			break;
		default:
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getPwmNum() {
		return gpio;
	}

	@Override
	public float getValue() {
		return value;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		this.value = value;
		int dc = (int) Math.floor(value * range);
		switch (pwmType) {
		case HARDWARE:
			Logger.info("setValue({}), range={}, dc={}", Float.valueOf(value), Integer.valueOf(range), Integer.valueOf(dc));
			Gpio.pwmWrite(gpio, dc);
			break;
		case SOFTWARE:
		default:
			SoftPwm.softPwmWrite(gpio, dc);
			break;
		}
	}
}
