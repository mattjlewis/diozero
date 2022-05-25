package com.diozero.internal.provider.pigpioj;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - pigpioj provider
 * Filename:     PigpioJServoDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;

import uk.pigpioj.PigpioInterface;

public class PigpioJServoDevice extends AbstractDevice implements InternalServoDeviceInterface {
	private PigpioInterface pigpioImpl;
	private int gpio;

	public PigpioJServoDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl, int gpio,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.gpio = gpio;

		setPulseWidthUs(initialPulseWidthUs);
	}

	@Override
	protected void closeDevice() {
		// TODO Nothing to do?
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getServoNum() {
		return gpio;
	}

	@Override
	public int getPulseWidthUs() throws RuntimeIOException {
		int pulse_width_us = pigpioImpl.getServoPulseWidth(gpio);
		if (pulse_width_us < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.getServoPulseWidth(), response: " + pulse_width_us);
		}

		return pulse_width_us;
	}

	@Override
	public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
		int rc = pigpioImpl.setServoPulseWidth(gpio, pulseWidthUs);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setServoPulseWidth(), response: " + rc);
		}
	}

	@Override
	public int getServoFrequency() {
		return 50;
	}

	@Override
	public void setServoFrequency(int frequencyHz) throws RuntimeIOException {
		throw new UnsupportedOperationException("pigpio does not allow servo frequency to be changed");
	}
}
