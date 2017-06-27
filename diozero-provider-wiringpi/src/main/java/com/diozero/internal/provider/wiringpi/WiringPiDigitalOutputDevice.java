package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - wiringPi provider
 * Filename:     WiringPiDigitalOutputDevice.java  
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

import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private int gpio;
	
	WiringPiDigitalOutputDevice(String key, DeviceFactoryInterface deviceFactory, int gpio, boolean initialValue) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.gpio = gpio;
		try {
			if (GpioUtil.isExported(gpio)) {
				GpioUtil.setDirection(gpio, initialValue ? GpioUtil.DIRECTION_HIGH : GpioUtil.DIRECTION_LOW);
			} else {
				GpioUtil.export(gpio, initialValue ? GpioUtil.DIRECTION_HIGH : GpioUtil.DIRECTION_LOW);
			}
			Gpio.pinMode(gpio, Gpio.OUTPUT);
		} catch (RuntimeException re) {
			throw new RuntimeIOException(re);
		}
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		GpioUtil.unexport(gpio);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return Gpio.digitalRead(gpio) == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		Gpio.digitalWrite(gpio, value);
	}

	@Override
	public int getGpio() {
		return gpio;
	}
}
