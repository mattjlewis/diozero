package com.diozero.api;

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

import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;
import com.diozero.internal.spi.GpioAnalogueInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class AnalogueInputDevice extends GpioInputDevice<AnalogueInputEvent> implements Runnable {
	private static final int DEFAULT_POLL_INTERVAL = 50;
	private GpioAnalogueInputDeviceInterface device;
	private Float lastValue;
	private int pollInterval = DEFAULT_POLL_INTERVAL;
	private float percentChange;
	private boolean stopScheduler;

	public AnalogueInputDevice(int pinNumber) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory().provisionAnalogueInputPin(pinNumber));
	}

	public AnalogueInputDevice(AnalogueInputDeviceFactoryInterface deviceFactory, int pinNumber) throws RuntimeIOException {
		this(deviceFactory.provisionAnalogueInputPin(pinNumber));
	}

	public AnalogueInputDevice(GpioAnalogueInputDeviceInterface device) {
		super(device.getPin());
		this.device = device;
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		stopScheduler = true;
		device.close();
	}
	
	public float getValue() throws RuntimeIOException {
		return device.getValue();
	}
	
	public void addListener(InputEventListener<AnalogueInputEvent> listener, float percentChange) {
		addListener(listener);
		this.percentChange = percentChange;
	}
	
	@Override
	public void enableListener() {
		GpioScheduler.getInstance().scheduleAtFixedRate(this, pollInterval, pollInterval, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void disableListener() {
		// TODO Implementation?
	}

	@Override
	public void run() {
		if (stopScheduler) {
			throw new RuntimeException("Stopping scheduler due to close request, device key=" + device.getKey());
		}
		
		float value = device.getValue();
		if (changeDetected(value)) {
			valueChanged(new AnalogueInputEvent(pinNumber, System.currentTimeMillis(), System.nanoTime(), value));
			lastValue = Float.valueOf(value);
		}
	}

	private boolean changeDetected(float value) {
		if (lastValue == null) {
			return true;
		}
		
		float last_value = lastValue.floatValue();
		if (percentChange != 0) {
			return value < (1 - percentChange) * last_value || value > (1 + percentChange) * last_value;
		}
		
		return value != last_value;
	}
}
