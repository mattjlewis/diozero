package com.diozero.internal.provider.jdkdio11;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
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

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.*;

public class JdkDeviceIoDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, PinListener {
	private GPIOPinConfig pinConfig;
	private GPIOPin pin;
	private long lastPinEventTime;
	private int debounceTimeMillis;
	
	JdkDeviceIoDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		super(key, deviceFactory);
		
		int mode;
		switch (pud) {
		case PULL_DOWN:
			mode = GPIOPinConfig.MODE_INPUT_PULL_DOWN;
			break;
		case PULL_UP:
			mode = GPIOPinConfig.MODE_INPUT_PULL_UP;
			break;
		default: 
			mode = DeviceConfig.UNASSIGNED;
		}
		
		int trig;
		switch (trigger) {
		case BOTH:
			trig = GPIOPinConfig.TRIGGER_BOTH_EDGES;
			break;
		case RISING:
			trig = GPIOPinConfig.TRIGGER_RISING_EDGE;
			break;
		case FALLING:
			trig = GPIOPinConfig.TRIGGER_FALLING_EDGE;
			break;
		default:
			trig = GPIOPinConfig.TRIGGER_NONE;
		}
		
		pinConfig = new GPIOPinConfig.Builder().setControllerNumber(DeviceConfig.UNASSIGNED).setPinNumber(gpio).
				setDirection(GPIOPinConfig.DIR_INPUT_ONLY).setDriveMode(mode).setTrigger(trig).build();
		try {
			pin = DeviceManager.open(GPIOPin.class, pinConfig);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		removeListener();
		if (pin.isOpen()) {
			try {
				pin.close();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
	
	// Exposed properties
	@Override
	public int getGpio() {
		return pinConfig.getPinNumber();
	}
	
	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			return pin.getValue();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		this.debounceTimeMillis = debounceTime;
	}

	@Override
	public void valueChanged(PinEvent event) {
		long nano_time = System.nanoTime();
		
		if (debounceTimeMillis == 0) {
			lastPinEventTime = event.getTimeStamp();

			valueChanged(new DigitalInputEvent(pinConfig.getPinNumber(),
					event.getTimeStamp(), nano_time, event.getValue()));
		} else {
			synchronized (this) {
				// FIXME This debounce functionality is a bit lacking, _all_ events are ignored for bounceTimeMillis
				// Important state change events might be missed, e.g. switch to 1 and immediate switch to 0
				long this_pin_event_time = event.getTimeStamp();
				if ((this_pin_event_time - lastPinEventTime) > debounceTimeMillis) {
					lastPinEventTime = this_pin_event_time;
	
					valueChanged(new DigitalInputEvent(pinConfig.getPinNumber(),
							event.getTimeStamp(), nano_time, event.getValue()));
				}
			}
		}
	}

	@Override
	public void enableListener() {
		try {
			pin.setInputListener(this);
		} catch (IOException e) {
			throw new IllegalStateException("I/O error calling setInputListener: " + e, e);
		}
	}

	@Override
	public void disableListener() {
		try {
			pin.setInputListener(null);
		} catch (IOException e) {
			throw new IllegalStateException("I/O error calling setInputListener: " + e, e);
		}
	}
}
