package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class SysFsAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent> implements AnalogInputDeviceInterface {
	private static final String DEVICE_PATH = "/sys/bus/iio/devices/iio:device";
	
	private int adcNumber;
	private RandomAccessFile voltageScale;
	private RandomAccessFile voltageRaw;
	
	public SysFsAnalogInputDevice(SysFsDeviceFactory deviceFactory, String key, int device, int adcNumber) {
		super(key, deviceFactory);
		
		this.adcNumber = adcNumber;
		
		Path device_path = FileSystems.getDefault().getPath(DEVICE_PATH + device);
		try {
			voltageScale = new RandomAccessFile(device_path.resolve("in_voltage_scale").toFile(), "r");
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
			voltageScale.seek(0);
			float scale = Float.parseFloat(voltageScale.readLine());
			voltageRaw.seek(0);
			float raw = Float.parseFloat(voltageRaw.readLine());
			
			return raw * scale;
		} catch (IOException | NumberFormatException e) {
			Logger.error("Error: {}" + e, e);
			throw new RuntimeIOException("Error reading analog input files: " + e, e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		try {
			voltageScale.close();
			voltageRaw.close();
		} catch (IOException e) {
		}
	}
}
