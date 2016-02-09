package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Device I/O Zero - wiringPi provider
 * %%
 * Copyright (C) 2016 diozero
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

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private int pinNumber;
	
	WiringPiDigitalOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, boolean initialValue) throws IOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		try {
			if (GpioUtil.isExported(pinNumber)) {
				GpioUtil.setDirection(pinNumber, initialValue ? GpioUtil.DIRECTION_HIGH : GpioUtil.DIRECTION_LOW);
			} else {
				GpioUtil.export(pinNumber, initialValue ? GpioUtil.DIRECTION_HIGH : GpioUtil.DIRECTION_LOW);
			}
			Gpio.pinMode(pinNumber, Gpio.OUTPUT);
		} catch (RuntimeException re) {
			throw new IOException(re);
		}
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		GpioUtil.unexport(pinNumber);
	}

	@Override
	public boolean getValue() throws IOException {
		return Gpio.digitalRead(pinNumber) == 1;
	}

	@Override
	public void setValue(boolean value) throws IOException {
		Gpio.digitalWrite(pinNumber, value);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
}
