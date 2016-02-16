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

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

public class DigitalOutputDevice extends GpioDevice {
	public static final int INFINITE_ITERATIONS = -1;
	
	private boolean activeHigh;
	private boolean running;
	private GpioDigitalOutputDeviceInterface device;
	
	public DigitalOutputDevice(int pinNumber) throws RuntimeIOException {
		this(pinNumber, true, false);
	}
	
	public DigitalOutputDevice(int pinNumber, boolean activeHigh, boolean initialValue) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, activeHigh, initialValue);
	}
	
	public DigitalOutputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber, boolean activeHigh, boolean initialValue) throws RuntimeIOException {
		this(deviceFactory.provisionDigitalOutputPin(pinNumber, activeHigh & initialValue), activeHigh);
	}

	public DigitalOutputDevice(GpioDigitalOutputDeviceInterface device, boolean activeHigh) {
		super(device.getPin());
		this.device = device;
		this.activeHigh = activeHigh;
	}

	@Override
	public void close() {
		Logger.debug("close()");
		setValue(!activeHigh);
		device.close();
	}
	
	protected void onOffLoop(float onTime, float offTime, int n, boolean background) throws RuntimeIOException {
		stopOnOffLoop();
		if (background) {
			DioZeroScheduler.getDaemonInstance().execute(() -> onOffLoop(onTime, offTime, n));
		} else {
			onOffLoop(onTime, offTime, n);
		}
	}
	
	private void onOffLoop(float onTime, float offTime, int n) throws RuntimeIOException {
		running = true;
		if (n > 0) {
			for (int i=0; i<n && running; i++) {
				onOff(onTime, offTime);
			}
		} else if (n == INFINITE_ITERATIONS) {
			while (running) {
				onOff(onTime, offTime);
			}
		}
	}

	private void onOff(float onTime, float offTime) throws RuntimeIOException {
		if (! running) {
			return;
		}
		setValueUnsafe(activeHigh);
		SleepUtil.sleepSeconds(onTime);
		
		if (!running) {
			return;
		}
		setValueUnsafe(!activeHigh);
		SleepUtil.sleepSeconds(offTime);
	}

	private void stopOnOffLoop() {
		// TODO Interrupt any background threads?
		running = false;
	}
	
	// Exposed operations
	public void on() throws RuntimeIOException {
		stopOnOffLoop();
		setValueUnsafe(activeHigh);
	}
	
	public void off() throws RuntimeIOException {
		stopOnOffLoop();
		setValueUnsafe(!activeHigh);
	}
	
	public void setOn(boolean on) throws RuntimeIOException {
		if (on) on(); else off();
	}
	
	public void toggle() throws RuntimeIOException {
		stopOnOffLoop();
		setValueUnsafe(!device.getValue());
	}

	// Exposed properties
	public boolean isOn() throws RuntimeIOException {
		return activeHigh == device.getValue();
	}
	
	public void setValue(boolean value) throws RuntimeIOException {
		stopOnOffLoop();
		setValueUnsafe(activeHigh ? value : !value);
	}
	
	/**
	 * Unsafe operation that has no synchronisation checks and doesn't factor in active low logic.
	 * Included primarily for performance tests
	 */
	public void setValueUnsafe(boolean value) throws RuntimeIOException {
		device.setValue(value);
	}
}
