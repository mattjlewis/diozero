package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
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

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.TemperatureSensorInterface;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * TMP36 temperature sensor
 */
public class TMP36 extends AnalogInputDevice implements TemperatureSensorInterface {
	private float tempOffset;

	public TMP36(AnalogInputDeviceFactoryInterface deviceFactory, int pinNumber,
			float vRef, float tempOffset) throws RuntimeIOException {
		super(deviceFactory, pinNumber, vRef);
		this.tempOffset = tempOffset;
	}
	
	@Override
	public float getScaledValue() throws RuntimeIOException {
		return getTemperature();
	}

	@Override
	public float getTemperature() throws RuntimeIOException {
		// Get the scaled value (voltage)
		float v = super.getScaledValue();
		return (100 * v - 50) + tempOffset;
	}
}
