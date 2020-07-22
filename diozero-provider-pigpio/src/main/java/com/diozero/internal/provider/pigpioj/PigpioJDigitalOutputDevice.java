package com.diozero.internal.provider.pigpioj;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - pigpioj provider
 * Filename:     PigpioJDigitalOutputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

import uk.pigpioj.PigpioConstants;
import uk.pigpioj.PigpioInterface;

public class PigpioJDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private PigpioInterface pigpioImpl;
	private int gpio;

	public PigpioJDigitalOutputDevice(String key, DeviceFactoryInterface deviceFactory,
			PigpioInterface pigpioImpl, int gpio, boolean initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.pigpioImpl = pigpioImpl;
		this.gpio = gpio;
		
		int rc = pigpioImpl.setMode(gpio, PigpioConstants.MODE_PI_OUTPUT);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setMode(), response: " + rc);
		}
		setValue(initialValue);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = pigpioImpl.read(gpio);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.read(), response: " + rc);
		}
		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		int rc = pigpioImpl.write(gpio, value);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.write(), response: " + rc);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		// No GPIO close method in pigpio
		// TODO Revert to default input mode?
	}
}
