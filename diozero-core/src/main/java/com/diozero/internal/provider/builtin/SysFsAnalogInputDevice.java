package com.diozero.internal.provider.builtin;

import java.io.File;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SysFsAnalogInputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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
import java.nio.file.Paths;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceInterface;

public class SysFsAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent>
		implements AnalogInputDeviceInterface {
	private static final String DEVICE_PATH = "/sys/bus/iio/devices/iio:device";

	private int adcNumber;
	private RandomAccessFile voltageScale;
	private RandomAccessFile voltageRaw;
	private float vRef;

	public SysFsAnalogInputDevice(DefaultDeviceFactory deviceFactory, String key, int device, PinInfo pinInfo) {
		super(key, deviceFactory);

		this.adcNumber = pinInfo.getDeviceNumber();
		vRef = pinInfo.getAdcVRef();

		Path device_path = Paths.get(DEVICE_PATH + device);
		File voltage_scale_file = device_path.resolve("in_voltage_scale").toFile();
		try {
			if (voltage_scale_file.exists()) {
				voltageScale = new RandomAccessFile(voltage_scale_file, "r");
			}
			voltageRaw = new RandomAccessFile(device_path.resolve("in_voltage" + adcNumber + "_raw").toFile(), "r");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening sysfs analog input files for ADC " + adcNumber, e);
		}
	}

	@Override
	public int getAdcNumber() {
		return adcNumber;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		try {
			voltageRaw.seek(0);
			float raw = Float.parseFloat(voltageRaw.readLine());

			float value;
			if (voltageScale != null) {
				voltageScale.seek(0);
				float scale = Float.parseFloat(voltageScale.readLine());
				value = raw * scale / vRef / 1000f;

				Logger.debug("raw: {}, scale: {}, vRef: {}", Float.valueOf(raw), Float.valueOf(scale),
						Float.valueOf(vRef));
			} else {
				// FIXME Needs to be passed in, this assumes 12-bit
				float range = (float) (Math.pow(2, 12) - 1);
				value = raw / range;

				Logger.debug("raw: {}, range: {}, vRef: {}", Float.valueOf(raw), Float.valueOf(range),
						Float.valueOf(vRef));
			}

			return value;
		} catch (IOException | NumberFormatException e) {
			Logger.error("Error: {}" + e, e);
			throw new RuntimeIOException("Error reading analog input files: " + e, e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		try {
			if (voltageScale != null) {
				voltageScale.close();
			}
			voltageRaw.close();
		} catch (IOException e) {
			// Ignore
		}
	}
}
