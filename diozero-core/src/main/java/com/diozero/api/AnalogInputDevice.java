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

import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.GpioAnalogInputDeviceInterface;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;

public class AnalogInputDevice extends GpioInputDevice<AnalogInputEvent> implements Runnable {
	private static final int DEFAULT_POLL_INTERVAL = 50;
	private GpioAnalogInputDeviceInterface device;
	private Float lastValue;
	private int pollInterval = DEFAULT_POLL_INTERVAL;
	private float percentChange;
	private boolean stopScheduler;
	private float range;

	public AnalogInputDevice(int pinNumber, float range) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, range);
	}

	public AnalogInputDevice(AnalogInputDeviceFactoryInterface deviceFactory, int pinNumber, float range) throws RuntimeIOException {
		super(pinNumber);
		device = deviceFactory.provisionAnalogInputPin(pinNumber);
		this.range = range;
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		stopScheduler = true;
		device.close();
	}
	
	/**
	 * Unscaled normalised values -1..1
	 * @return the unscaled value
	 * @throws RuntimeIOException if there was an I/O error
	 */
	public float getUnscaledValue() throws RuntimeIOException {
		return device.getValue();
	}
	
	/**
	 * Scaled values -range..range
	 * @return the scaled value
	 * @throws RuntimeIOException if there was an I/O error
	 */
	public float getScaledValue() throws RuntimeIOException {
		// The raw device must return unscaled values (-1..1)
		return device.getValue() * range;
	}
	
	public void addListener(InputEventListener<AnalogInputEvent> listener, float percentChange) {
		addListener(listener, percentChange, DEFAULT_POLL_INTERVAL);
	}
	
	public void addListener(InputEventListener<AnalogInputEvent> listener, float percentChange, int pollInterval) {
		this.percentChange = percentChange;
		this.pollInterval = pollInterval;
		addListener(listener);
	}
	
	@Override
	protected void enableListener() {
		DioZeroScheduler.getDaemonInstance().scheduleAtFixedRate(this, pollInterval, pollInterval, TimeUnit.MILLISECONDS);
	}
	
	@Override
	protected void disableListener() {
		stopScheduler = true;
	}

	@Override
	public void run() {
		if (stopScheduler) {
			throw new RuntimeException("Stopping scheduler due to close request, device key=" + device.getKey());
		}
		
		float unscaled = getUnscaledValue();
		if (changeDetected(unscaled)) {
			valueChanged(new AnalogInputEvent(pinNumber, System.currentTimeMillis(), System.nanoTime(), unscaled, unscaled*range));
			lastValue = Float.valueOf(unscaled);
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
