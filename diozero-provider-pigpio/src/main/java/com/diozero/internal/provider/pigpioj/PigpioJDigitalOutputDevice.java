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
		
		try {
			PigpioGpio.setMode(pinNumber, PigpioGpio.MODE_PI_OUTPUT);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		setValue(initialValue);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			return PigpioGpio.read(pinNumber);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		try {
			PigpioGpio.write(pinNumber, value);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		// No GPIO close method in pigpio
	}
}
