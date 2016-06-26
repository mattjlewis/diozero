package com.diozero.internal.provider.pigpioj;

/*
 * #%L
 * Device I/O Zero - pigpioj provider
 * %%
 * Copyright (C) 2016 mattjlewis
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
		
		int rc = PigpioGpio.setMode(pinNumber, PigpioGpio.MODE_PI_OUTPUT);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.setMode(), response: " + rc);
		}
		setValue(initialValue);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = PigpioGpio.read(pinNumber);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.read(), response: " + rc);
		}
		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		int rc = PigpioGpio.write(pinNumber, value);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.write(), response: " + rc);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		// No GPIO close method in pigpio
		// TODO Revert to default input mode?
	}
}
