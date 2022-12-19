package com.diozero.internal;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PwmServoDevice.java
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

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;

public class PwmServoDevice extends AbstractDevice implements InternalServoDeviceInterface {
	private InternalPwmOutputDeviceInterface pwmOutputDevice;
	private int periodUs;

	public PwmServoDevice(String key, DeviceFactoryInterface deviceFactory,
			InternalPwmOutputDeviceInterface pwmOutputDevice, int minPulseWidthUs, int maxPulseWidthUs,
			int initialPulseWidthUs) {
		super(key, deviceFactory);

		this.pwmOutputDevice = pwmOutputDevice;
		pwmOutputDevice.setChild(true);
		periodUs = 1_000_000 / pwmOutputDevice.getPwmFrequency();

		setPulseWidthUs(initialPulseWidthUs);
	}

	@Override
	public int getGpio() {
		return pwmOutputDevice.getGpio();
	}

	@Override
	public int getServoNum() {
		return pwmOutputDevice.getPwmNum();
	}

	@Override
	public int getPulseWidthUs() throws RuntimeIOException {
		return Math.round(pwmOutputDevice.getValue() * periodUs);
	}

	@Override
	public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
		pwmOutputDevice.setValue(pulseWidthUs / (float) periodUs);
	}

	@Override
	public int getServoFrequency() {
		return pwmOutputDevice.getPwmFrequency();
	}

	@Override
	public void setServoFrequency(int frequencyHz) throws RuntimeIOException {
		pwmOutputDevice.setPwmFrequency(frequencyHz);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		pwmOutputDevice.close();
	}
}
