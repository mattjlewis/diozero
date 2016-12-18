package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
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


import com.diozero.api.AnalogInputDevice;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;

/**
 * Supports taking readings from a <a href="https://en.wikipedia.org/wiki/Potentiometer">potentiometer</a>.
 */
public class Potentiometer extends AnalogInputDevice {
	/**
	 * @param gpio GPIO which the potentiometer is connected.
	 * @param vRef Reference voltage.
	 */
	public Potentiometer(int gpio, float vRef) {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, vRef);
	}
	
	public Potentiometer(AnalogInputDeviceFactoryInterface deviceFactory, int gpio, float vRef) {
		super(deviceFactory, gpio, vRef);
	}
	
	public float getVoltage() {
		return getScaledValue();
	}
}
