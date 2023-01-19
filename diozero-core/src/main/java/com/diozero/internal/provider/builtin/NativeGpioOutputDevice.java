package com.diozero.internal.provider.builtin;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     NativeGpioOutputDevice.java
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

import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.MmapGpioInterface;

public class NativeGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private int gpio;
	private GpioLine line;
	private MmapGpioInterface mmapGpio;

	public NativeGpioOutputDevice(DefaultDeviceFactory deviceFactory, String key, GpioChip chip, PinInfo pinInfo,
			boolean initialValue, MmapGpioInterface mmapGpio) {
		super(key, deviceFactory);
		
		this.mmapGpio = mmapGpio;

		gpio = pinInfo.getDeviceNumber();
		int offset = pinInfo.getLineOffset();
		if (offset == PinInfo.NOT_DEFINED) {
			throw new IllegalArgumentException("Line offset not defined for pin " + pinInfo);
		}

		line = chip.provisionGpioOutputDevice(offset, initialValue ? 1 : 0);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		if (mmapGpio == null) {
			return line.getValue() == 0 ? false : true;
		}
		return mmapGpio.gpioRead(gpio);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mmapGpio == null) {
			line.setValue(value ? 1 : 0);
		} else {
			mmapGpio.gpioWrite(gpio, value);
		}
	}

	@Override
	public void closeDevice() {
		line.close();
	}
}
