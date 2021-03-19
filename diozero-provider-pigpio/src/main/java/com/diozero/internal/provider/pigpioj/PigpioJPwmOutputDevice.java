package com.diozero.internal.provider.pigpioj;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - pigpioj provider
 * Filename:     PigpioJPwmOutputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;

import uk.pigpioj.PigpioInterface;

public class PigpioJPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private PigpioInterface pigpioImpl;
	private int gpio;
	private int range;

	public PigpioJPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl,
			int gpio, float initialValue, int range) {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.gpio = gpio;
		this.range = range;
		
		setValue(initialValue);
	}

	@Override
	protected void closeDevice() {
		// TODO Nothing to do?
	}

	@Override
	public int getGpio() {
		return gpio;
	}
	
	@Override
	public int getPwmNum() {
		return gpio;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		int dc = pigpioImpl.getPWMDutyCycle(gpio);
		if (dc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.getPWMDutyCycle(), response: " + dc);
		}
		
		return dc / (float) range;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		int rc = pigpioImpl.setPWMDutyCycle(gpio, Math.round(range * value));
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setPWMDutyCycle(), response: " + rc);
		}
	}

	@Override
	public int getPwmFrequency() {
		int frequency = pigpioImpl.getPWMFrequency(gpio);
		if (frequency < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.getPWMFrequency(), response: " + frequency);
		}
		return frequency;
	}

	@Override
	public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
		int rc = pigpioImpl.setPWMFrequency(gpio, frequencyHz);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setPWMFrequency(), response: " + rc);
		}
	}
}
