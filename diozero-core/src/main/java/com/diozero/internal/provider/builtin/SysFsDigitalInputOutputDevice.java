package com.diozero.internal.provider.builtin;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SysFsDigitalInputOutputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.builtin.gpio.SysFsGpioUtil;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.PollEventListener;

public class SysFsDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface, PollEventListener {
	private static final String VALUE_FILE = "value";
	private static final byte LOW_VALUE = '0';
	private static final byte HIGH_VALUE = '1';

	private DefaultDeviceFactory deviceFactory;
	protected int gpio;
	private Path valuePath;
	private RandomAccessFile valueFile;
	protected DeviceMode mode;

	public SysFsDigitalInputOutputDevice(DefaultDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			DeviceMode mode) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		this.gpio = pinInfo.getSysFsNumber();
		Path gpio_dir = SysFsGpioUtil.getGpioDirectoryPath(gpio);

		setMode(mode);

		valuePath = gpio_dir.resolve(VALUE_FILE);
		try {
			valueFile = new RandomAccessFile(valuePath.toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening value file '" + valuePath + "' for GPIO " + gpio, e);
		}
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		SysFsGpioUtil.export(gpio, mode);
		this.mode = mode;
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
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
		// TODO Throw error if mode is not DIGITAL_OUTPUT
		try {
			valueFile.seek(0);
			valueFile.writeByte(value ? HIGH_VALUE : LOW_VALUE);
		} catch (IOException e) {
			throw new RuntimeIOException("Error writing value", e);
		}
	}

	@Override
	protected void enableListener() {
		deviceFactory.getEpoll().register(valuePath.toString(), this);
	}

	@Override
	protected void disableListener() {
		deviceFactory.getEpoll().deregister(valuePath.toString());
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		disableListener();
		try {
			valueFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		SysFsGpioUtil.unexport(gpio);
	}

	@Override
	public void notify(long epochTime, long nanoTime, char value) {
		valueChanged(new DigitalInputEvent(gpio, epochTime, nanoTime, value == HIGH_VALUE));
	}
}
