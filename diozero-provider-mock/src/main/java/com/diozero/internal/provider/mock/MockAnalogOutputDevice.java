package com.diozero.internal.provider.mock;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Mock provider
 * Filename:     MockAnalogOutputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import com.diozero.internal.spi.AnalogOutputDeviceInterface;

public class MockAnalogOutputDevice extends AbstractDevice implements AnalogOutputDeviceInterface {
	private static final int INT_RANGE = 256;

	private final MockDeviceFactory deviceFactory;
	private final MockGpio mock;

	public MockAnalogOutputDevice(String key, MockDeviceFactory deviceFactory, int gpio, float initialValue) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		mock = deviceFactory.provisionGpio(gpio, DeviceMode.ANALOG_OUTPUT, (int) initialValue * INT_RANGE);
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice() {}", getKey());
		deviceFactory.deprovisionGpio(mock);
	}

	@Override
	public int getAdcNumber() {
		return mock.getGpio();
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return mock.getValue() / (float) INT_RANGE;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		mock.setValue((int) value * INT_RANGE);
	}
}
