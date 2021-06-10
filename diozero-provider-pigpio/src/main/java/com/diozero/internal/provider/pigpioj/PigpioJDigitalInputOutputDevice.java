package com.diozero.internal.provider.pigpioj;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - pigpioj provider
 * Filename:     PigpioJDigitalInputOutputDevice.java
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

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;

import uk.pigpioj.PigpioCallback;
import uk.pigpioj.PigpioConstants;
import uk.pigpioj.PigpioInterface;

public class PigpioJDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface, PigpioCallback {
	private PigpioInterface pigpioImpl;
	private DeviceMode mode;
	private int gpio;

	public PigpioJDigitalInputOutputDevice(String key, PigpioJDeviceFactory deviceFactory, PigpioInterface pigpioImpl,
			int gpio, DeviceMode mode) {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.gpio = gpio;

		setMode(mode);
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		if (mode == DeviceMode.DIGITAL_INPUT) {
			int rc = pigpioImpl.setMode(gpio, PigpioConstants.MODE_PI_INPUT);
			if (rc < 0) {
				throw new RuntimeIOException("Error calling pigpioImpl.setMode(), response: " + rc);
			}
			rc = pigpioImpl.setPullUpDown(gpio, PigpioJDeviceFactory.getPigpioJPullUpDown(GpioPullUpDown.NONE));
			if (rc < 0) {
				throw new RuntimeIOException("Error calling pigpioImpl.setPullUpDown(), response: " + rc);
			}
		} else {
			int rc = pigpioImpl.setMode(gpio, PigpioConstants.MODE_PI_OUTPUT);
			if (rc < 0) {
				throw new RuntimeIOException("Error calling pigpioImpl.setMode(), response: " + rc);
			}
		}

		this.mode = mode;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = pigpioImpl.read(gpio);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.read(), response: " + rc);
		}
		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		int rc = pigpioImpl.write(gpio, value);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.write(), response: " + rc);
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		// FIXME No piogpio close method?
		// TODO Revert to default input mode?
		super.closeDevice();
	}

	@Override
	public void enableListener() {
		disableListener();
		int rc = pigpioImpl.enableListener(gpio, PigpioConstants.EITHER_EDGE, this);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setISRFunc(), response: " + rc);
		}
	}

	@Override
	public void disableListener() {
		int rc = pigpioImpl.disableListener(gpio);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setISRFunc(), response: " + rc);
		}
	}

	@Override
	public void callback(int pin, boolean value, long epochTime, long nanoTime) {
		if (pin != gpio) {
			Logger.error("Error, got a callback for the wrong pin ({}), was expecting {}", Integer.valueOf(pin),
					Integer.valueOf(gpio));
		}

		accept(new DigitalInputEvent(pin, epochTime, nanoTime, value));
	}
}
