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

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

import uk.pigpioj.PigpioCallback;
import uk.pigpioj.PigpioConstants;
import uk.pigpioj.PigpioInterface;

public class PigpioJDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputDeviceInterface, PigpioCallback {
	private PigpioInterface pigpioImpl;
	private int gpio;
	private int edge;

	public PigpioJDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl,
			int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.pigpioImpl = pigpioImpl;
		this.gpio = gpio;
		
		switch (trigger) {
		case RISING:
			edge = PigpioConstants.RISING_EDGE;
			break;
		case FALLING:
			edge = PigpioConstants.FALLING_EDGE;
			break;
		case BOTH:
			edge = PigpioConstants.EITHER_EDGE;
			break;
		case NONE:
		default:
			edge = PigpioConstants.NO_EDGE;
		}
		int pigpio_pud = PigpioJDeviceFactory.getPigpioJPullUpDown(pud);
		
		int rc = pigpioImpl.setMode(gpio, PigpioConstants.MODE_PI_INPUT);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setMode(), response: " + rc);
		}
		rc = pigpioImpl.setPullUpDown(gpio, pigpio_pud);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setPullUpDown(), response: " + rc);
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		// No GPIO close method in pigpio
		removeListener();
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
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported in pigpioj");
	}

	@Override
	public void enableListener() {
		disableListener();
		if (edge == PigpioConstants.NO_EDGE) {
			Logger.warn("Edge was configured to be NO_EDGE, no point adding a listener");
			return;
		}
		
		int rc = pigpioImpl.enableListener(gpio, edge, this);
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
			Logger.error("Error, got a callback for the wrong pin ({}), was expecting {}",
					Integer.valueOf(pin), Integer.valueOf(gpio));
		}
		
		valueChanged(new DigitalInputEvent(pin, epochTime, nanoTime, value));
	}
}
