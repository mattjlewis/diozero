package com.diozero.internal.provider.pi4j;

/*
 * #%L
 * Device I/O Zero - pi4j provider
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
import com.pi4j.io.gpio.*;

public class Pi4jPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private GpioPinPwmOutput pwmOutputPin;
	private int pinNumber;
	private float value;
	private int range;
	
	Pi4jPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, GpioController gpioController,
			PwmType pwmType, int range, int pinNumber, float initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		Pin pin = RaspiGpioBcm.getPin(pinNumber);
		if (pin == null) {
			throw new IllegalArgumentException("Illegal pin number: " + pinNumber);
		}
		
		this.pinNumber = pinNumber;
		this.value = initialValue;
		this.range = range;
		
		switch (pwmType) {
		case HARDWARE:
			pwmOutputPin = gpioController.provisionPwmOutputPin(pin, "PWM output for BCM GPIO " + pinNumber,
					(int)(value * range));
			break;
		case SOFTWARE:
			pwmOutputPin = gpioController.provisionSoftPwmOutputPin(
					pin, "PWM output for BCM GPIO " + pinNumber, (int)(initialValue * range));
			/*
			int status = SoftPwm.softPwmCreate(pinNumber, (int)(initialValue * range), range);
			if (status != 0) {
				throw new RuntimeIOException("Error setting up software controlled PWM GPIO on BCM pin " +
						pinNumber + ", status=" + status);
			}
			*/
		}
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		GpioFactory.getInstance().unprovisionPin(pwmOutputPin);
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
		pwmOutputPin.setPwm((int)(value * range));
		/*
		switch (pwmType) {
		case HARDWARE:
			pwmOutputPin.setPwm((int)(value * range));
			break;
		case SOFTWARE:
		default:
			SoftPwm.softPwmWrite(pinNumber, (int)(value * range));
			break;
		}
		*/
	}
}
