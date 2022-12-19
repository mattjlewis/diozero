package com.diozero.internal.provider.mock;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Mock provider
 * Filename:     MockServoDevice.java
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

import com.diozero.api.DeviceMode;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalServoDeviceInterface;

public class MockServoDevice extends AbstractDevice implements InternalServoDeviceInterface {
	private final MockDeviceFactory deviceFactory;
	private final MockGpio mock;
	private int servoNum;
	private int frequencyHz;
	private int minPulseWidthUs;
	private int maxPulseWidthUs;
	private int pulseWidthUs;

	public MockServoDevice(String key, MockDeviceFactory deviceFactory, int gpio, int servoNum, int frequencyHz,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		mock = deviceFactory.provisionGpio(gpio, DeviceMode.SERVO, 0);
		this.servoNum = servoNum;
		this.frequencyHz = frequencyHz;
		this.minPulseWidthUs = minPulseWidthUs;
		this.maxPulseWidthUs = maxPulseWidthUs;
		this.pulseWidthUs = initialPulseWidthUs;
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice() {}", getKey());
		deviceFactory.deprovisionGpio(mock);
	}

	@Override
	public int getPulseWidthUs() throws RuntimeIOException {
		return pulseWidthUs;
	}

	@Override
	public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
		Logger.debug("setAngle({})", Integer.valueOf(pulseWidthUs));
		this.pulseWidthUs = pulseWidthUs;
	}

	@Override
	public int getGpio() {
		return mock.getGpio();
	}

	@Override
	public int getServoNum() {
		return servoNum;
	}

	public int getMinPulseWisthUs() {
		return minPulseWidthUs;
	}

	public int getMaxPulseWisthUs() {
		return maxPulseWidthUs;
	}

	@Override
	public int getServoFrequency() {
		return frequencyHz;
	}

	@Override
	public void setServoFrequency(int frequencyHz) throws RuntimeIOException {
		this.frequencyHz = frequencyHz;
	}
}
