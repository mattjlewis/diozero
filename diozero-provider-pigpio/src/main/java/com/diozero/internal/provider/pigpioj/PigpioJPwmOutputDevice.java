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

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.pigpioj.PigpioGpio;
import com.diozero.util.RuntimeIOException;

public class PigpioJPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private int pinNumber;
	private int range;

	public PigpioJPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			float initialValue, int range) {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		this.range = range;
		
		setValue(initialValue);
	}

	@Override
	public void closeDevice() {
		// Nothing to do?
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		int dc = PigpioGpio.getPWMDutyCycle(pinNumber);
		if (dc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.getPWMDutyCycle(), response: " + dc);
		}
		
		return dc / (float)range;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		int rc = PigpioGpio.setPWMDutyCycle(pinNumber, (int)(range*value));
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.setPWMDutyCycle(), response: " + rc);
		}
	}
}
