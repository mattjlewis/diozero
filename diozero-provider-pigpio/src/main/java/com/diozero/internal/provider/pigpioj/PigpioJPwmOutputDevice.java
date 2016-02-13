package com.diozero.internal.provider.pigpioj;

/*
 * #%L
 * Device I/O Zero - pigpioj provider
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


import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.pigpioj.PigpioGpio;
import com.diozero.util.RuntimeIOException;

public class PigpioJPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final int PWM_RANGE = 256-1;
	
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
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error setting value to 0 in closeDevice(): {}", e);
		}
		// Nothing more to do?
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		try {
			return PigpioGpio.getPWMDutyCycle(pinNumber) / (float)PWM_RANGE;
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		try {
			PigpioGpio.setPWMDutyCycle(pinNumber, (int)(PWM_RANGE*value));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
