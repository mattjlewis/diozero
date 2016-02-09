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


import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.*;
import com.diozero.pigpioj.PigpioCallback;
import com.diozero.pigpioj.PigpioGpio;

public class PigpioJDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface, PigpioCallback {
	private int pinNumber;
	private int edge;
	private InternalPinListener listener;

	public PigpioJDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
		super(key, deviceFactory);
		
		switch (trigger) {
		case RISING:
			edge = PigpioGpio.RISING_EDGE;
			break;
		case FALLING:
			edge = PigpioGpio.FALLING_EDGE;
			break;
		case BOTH:
			edge = PigpioGpio.EITHER_EDGE;
			break;
		case NONE:
		default:
			edge = PigpioGpio.NO_EDGE;
		}
		
		int pigpio_pud;
		switch (pud) {
		case PULL_DOWN:
			pigpio_pud = PigpioGpio.PI_PUD_DOWN;
			break;
		case PULL_UP:
			pigpio_pud = PigpioGpio.PI_PUD_UP;
			break;
		case NONE:
		default:
			pigpio_pud = PigpioGpio.PI_PUD_OFF;
			break;
		}
		
		PigpioGpio.setMode(pinNumber, PigpioGpio.MODE_PI_INPUT);
		PigpioGpio.setPullUpDown(pinNumber, pigpio_pud);
		
		this.pinNumber = pinNumber;
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void closeDevice() throws IOException {
		// No GPIO close method in pigpio
		removeListener();
	}

	@Override
	public boolean getValue() throws IOException {
		return PigpioGpio.read(pinNumber);
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported in pigpioj");
	}

	@Override
	public void setListener(InternalPinListener listener) {
		if (edge == PigpioGpio.NO_EDGE) {
			Logger.warn("Edge was configured to be NO_EDGE, no point adding a listener");
			return;
		}
		
		if (this.listener != null) {
			removeListener();
		}
		
		this.listener = listener;
		try {
			PigpioGpio.setISRFunc(pinNumber, edge, -1, this);
		} catch (IOException e) {
			Logger.error(e, "Error setting listener: {}", e);
		}
	}

	@Override
	public void removeListener() {
		try {
			PigpioGpio.setISRFunc(pinNumber, PigpioGpio.EITHER_EDGE, -1, null);
		} catch (IOException e) {
			Logger.warn(e, "Error removing listener: {}", e);
		}
		listener = null;
	}

	@Override
	public void callback(int pin, boolean value, long epochTime, long nanoTime) {
		if (pin != pinNumber) {
			Logger.error("Error, got a callback for the wrong pin ({}), was expecting {}",
					Integer.valueOf(pin), Integer.valueOf(pinNumber));
		}
		
		if (listener != null) {
			listener.valueChanged(new DigitalPinEvent(pin, epochTime, nanoTime, value));
		}
	}
}
