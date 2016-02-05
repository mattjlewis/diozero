package com.diozero.internal.provider.jdkdio10;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.*;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.*;

public class JdkDeviceIoGpioInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface, PinListener {
	private static final Logger logger = LogManager.getLogger(JdkDeviceIoGpioInputDevice.class);
	
	private GPIOPinConfig pinConfig;
	private GPIOPin pin;
	private InternalPinListener listener;
	private long lastPinEventTime;
	private int debounceTimeMillis;
	
	JdkDeviceIoGpioInputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
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
			mode = GPIOPinConfig.DEFAULT;
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
		
		pinConfig = new GPIOPinConfig(DeviceConfig.DEFAULT, pinNumber, GPIOPinConfig.DIR_INPUT_ONLY, mode, trig, false);
		pin = DeviceManager.open(GPIOPin.class, pinConfig);
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		removeListener();
		if (pin.isOpen()) {
			pin.close();
		}
	}
	
	// Exposed properties
	@Override
	public int getPin() {
		return pinConfig.getPinNumber();
	}
	
	@Override
	public boolean getValue() throws IOException {
		return pin.getValue();
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

			if (listener != null) {
				listener.valueChanged(new DigitalPinEvent(
						pinConfig.getPinNumber(), event.getTimeStamp(), nano_time, event.getValue()));
			}
		} else {
			synchronized (this) {
				// FIXME This debounce functionality is a bit lacking, _all_ events are ignored for bounceTimeMillis
				// Important state change events might be missed, e.g. switch to 1 and immediate switch to 0
				long this_pin_event_time = event.getTimeStamp();
				if ((this_pin_event_time - lastPinEventTime) > debounceTimeMillis) {
					lastPinEventTime = this_pin_event_time;
	
					if (listener != null) {
						listener.valueChanged(new DigitalPinEvent(
								pinConfig.getPinNumber(), event.getTimeStamp(), nano_time, event.getValue()));
					}
				}
			}
		}
	}

	@Override
	public void setListener(InternalPinListener listener) {
		this.listener = listener;
		try {
			pin.setInputListener(this);
		} catch (IOException e) {
			throw new IllegalStateException("I/O error calling setInputListener: " + e, e);
		}
	}

	@Override
	public void removeListener() {
		try { pin.setInputListener(null); } catch (IOException e) { }
		listener = null;
	}
}
