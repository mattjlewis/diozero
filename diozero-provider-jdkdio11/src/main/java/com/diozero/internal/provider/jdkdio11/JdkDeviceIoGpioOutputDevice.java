package com.diozero.internal.provider.jdkdio11;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - JDK Device I/O v1.1 provider
 * Filename:     JdkDeviceIoGpioOutputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

public class JdkDeviceIoGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private GPIOPinConfig pinConfig;
	private GPIOPin pin;
	
	JdkDeviceIoGpioOutputDevice(String key, DeviceFactoryInterface deviceFactory, int gpio, boolean initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		pinConfig = new GPIOPinConfig.Builder().setControllerNumber(DeviceConfig.UNASSIGNED).setPinNumber(gpio)
				.setDirection(GPIOPinConfig.DIR_OUTPUT_ONLY).setDriveMode(DeviceConfig.UNASSIGNED)
				.setTrigger(GPIOPinConfig.TRIGGER_NONE).setInitValue(initialValue).build();
				//GPIOPinConfig.MODE_OUTPUT_PUSH_PULL, GPIOPinConfig.TRIGGER_NONE, false);
		try {
			pin = DeviceManager.open(GPIOPin.class, pinConfig);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		if (pin.isOpen()) {
			try {
				pin.close();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
	
	// Exposed properties
	@Override
	public int getGpio() {
		return pinConfig.getPinNumber();
	}
	
	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			return pin.getValue();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		try {
			pin.setValue(value);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
}
