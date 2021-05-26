package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataPwmOutputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.PwmOutputDeviceInterface;

public class FirmataPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final float PWM_MAX = 255;
	
	private Pin pin;
	
	public FirmataPwmOutputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber,
			float initialValue) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		try {
			pin.setMode(Mode.PWM);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting pin mode to PWM for pin " + deviceNumber);
		}
		setValue(initialValue);
	}

	@Override
	public int getGpio() {
		return pin.getIndex();
	}

	@Override
	public int getPwmNum() {
		return pin.getIndex();
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return pin.getValue() / PWM_MAX;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		try {
			pin.setValue(Math.round(value * PWM_MAX));
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting PWM value to " + value + " for pin " + pin.getIndex());
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		setValue(0);
		// TODO Anything else to do?
	}

	@Override
	public int getPwmFrequency() {
		throw new UnsupportedOperationException("Actual PWM frequency varies by board");
	}

	@Override
	public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
		throw new UnsupportedOperationException("Unable to change PWM frequency");
	}
}
