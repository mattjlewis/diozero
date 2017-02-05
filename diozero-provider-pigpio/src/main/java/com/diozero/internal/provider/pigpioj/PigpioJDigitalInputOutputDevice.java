package com.diozero.internal.provider.pigpioj;

/*
 * #%L
 * Device I/O Zero - pigpioj provider
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

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.pigpioj.PigpioGpio;
import com.diozero.util.RuntimeIOException;

public class PigpioJDigitalInputOutputDevice extends AbstractDevice
implements GpioDigitalInputOutputDeviceInterface {
	private DeviceMode mode;
	private int gpio;

	public PigpioJDigitalInputOutputDevice(String key, PigpioJDeviceFactory deviceFactory,
			int gpio, DeviceMode mode) {
		super(key, deviceFactory);
		
		this.gpio = gpio;
		
		setMode(mode);
	}
	
	private static void checkMode(DeviceMode mode) {
		if (mode != DeviceMode.DIGITAL_INPUT && mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Invalid mode, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
		}
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		checkMode(mode);
		
		// No change?
		if (this.mode != null && mode == this.mode) {
			return;
		}

		if (mode == DeviceMode.DIGITAL_INPUT) {
			int rc = PigpioGpio.setMode(gpio, PigpioGpio.MODE_PI_INPUT);
			if (rc < 0) {
				throw new RuntimeIOException("Error calling PigpioGpio.setMode(), response: " + rc);
			}
			rc = PigpioGpio.setPullUpDown(gpio, PigpioJDeviceFactory.getPigpioJPullUpDown(GpioPullUpDown.NONE));
			if (rc < 0) {
				throw new RuntimeIOException("Error calling PigpioGpio.setPullUpDown(), response: " + rc);
			}
		} else {
			int rc = PigpioGpio.setMode(gpio, PigpioGpio.MODE_PI_OUTPUT);
			if (rc < 0) {
				throw new RuntimeIOException("Error calling PigpioGpio.setMode(), response: " + rc);
			}
		}
		
		this.mode = mode;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = PigpioGpio.read(gpio);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.read(), response: " + rc);
		}
		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		int rc = PigpioGpio.write(gpio, value);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.write(), response: " + rc);
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		// FIXME No piogpio close method?
		// TODO Revert to default input mode?
	}
}
