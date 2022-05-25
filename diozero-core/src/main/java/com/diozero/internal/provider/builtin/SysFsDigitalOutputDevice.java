package com.diozero.internal.provider.builtin;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SysFsDigitalOutputDevice.java
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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.builtin.gpio.SysFsGpioUtil;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.MmapGpioInterface;

public class SysFsDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final String VALUE_FILE = "value";
	private static final byte LOW_VALUE = '0';
	private static final byte HIGH_VALUE = '1';

	private MmapGpioInterface mmapGpio;
	private int gpio;
	private RandomAccessFile valueFile;

	public SysFsDigitalOutputDevice(DefaultDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			boolean initialValue, MmapGpioInterface mmapGpio) {
		super(key, deviceFactory);
		
		this.mmapGpio = mmapGpio;
		this.gpio = pinInfo.getSysFsNumber();

		SysFsGpioUtil.export(pinInfo.getSysFsNumber(), DeviceMode.DIGITAL_OUTPUT);

		Path gpio_dir = SysFsGpioUtil.getGpioDirectoryPath(gpio);

		// TODO Set active_low value to 0

		try {
			valueFile = new RandomAccessFile(gpio_dir.resolve(VALUE_FILE).toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening value file for GPIO " + gpio, e);
		}

		setValue(initialValue);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		if (mmapGpio != null) {
			return mmapGpio.gpioRead(gpio);
		}
		
		try {
			// Note seek(0) is required
			valueFile.seek(0);
			return valueFile.readByte() == HIGH_VALUE;
		} catch (IOException e) {
			throw new RuntimeIOException("Error reading value", e);
		}
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mmapGpio != null) {
			mmapGpio.gpioWrite(gpio, value);
		} else {
			try {
				valueFile.seek(0);
				valueFile.write(value ? HIGH_VALUE : LOW_VALUE);
			} catch (IOException e) {
				throw new RuntimeIOException("Error writing value", e);
			}
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		try {
			valueFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		SysFsGpioUtil.unexport(gpio);
	}
}
