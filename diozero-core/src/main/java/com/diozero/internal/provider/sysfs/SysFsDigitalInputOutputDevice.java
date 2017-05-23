package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Java Sysfs provider
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


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class SysFsDigitalInputOutputDevice extends AbstractDevice implements GpioDigitalInputOutputDeviceInterface {
	private static final String VALUE_FILE = "value";
	private static final byte LOW_VALUE = '0';
	private static final byte HIGH_VALUE = '1';

	private SysFsDeviceFactory deviceFactory;
	private int gpio;
	private RandomAccessFile valueFile;
	private DeviceMode mode;

	public SysFsDigitalInputOutputDevice(SysFsDeviceFactory deviceFactory, Path gpioDir, String key, int gpio,
			DeviceMode mode) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		this.gpio = gpio;
		
		try {
			valueFile = new RandomAccessFile(gpioDir.resolve(VALUE_FILE).toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening value file for GPIO " + gpio, e);
		}
		
		setMode(mode);
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}
	
	@Override
	public void setMode(DeviceMode mode) {
		deviceFactory.export(gpio, mode);
		this.mode = mode;
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			valueFile.seek(0);
			return valueFile.readByte() == HIGH_VALUE;
		} catch (IOException e) {
			throw new RuntimeIOException("Error reading value", e);
		}
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		// TODO Throw error if mode is not DIGITAL_OUTPUT
		try {
			valueFile.seek(0);
			valueFile.writeByte(value ? HIGH_VALUE : LOW_VALUE);
		} catch (IOException e) {
			throw new RuntimeIOException("Error writing value", e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		try {
			valueFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		deviceFactory.unexport(gpio);
	}
}
