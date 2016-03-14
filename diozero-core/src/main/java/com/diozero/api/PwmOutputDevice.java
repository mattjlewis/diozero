package com.diozero.api;

import java.io.IOException;

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

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Represent a generic PWM output GPIO.
 * Note the following BCM GPIO pins provide hardware PWM support:
 * 	12 (phys 32, wPi 26), 13 (phys 33, wPi 23), 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
 * Any other pin will revert to software controlled PWM (not very good)
 */
public class PwmOutputDevice extends GpioDevice {
	public static final int INFINITE_ITERATIONS = -1;
	
	private PwmOutputDeviceInterface device;
	private boolean running;
	private Thread backgroundThread;

	public PwmOutputDevice(int pinNumber) throws RuntimeIOException {
		this(pinNumber, 0);
	}
	
	public PwmOutputDevice(int pinNumber, float initialValue) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, initialValue);
	}
	
	public PwmOutputDevice(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int pinNumber,
			float initialValue) throws RuntimeIOException {
		super(pinNumber);
		this.device = pwmDeviceFactory.provisionPwmOutputPin(pinNumber, initialValue);
	}

	@Override
	public void close() {
		Logger.debug("close()");
		stopLoops();
		if (backgroundThread != null) {
			Logger.info("Interrupting background thread " + backgroundThread.getName());
			backgroundThread.interrupt();
		}
		Logger.info("Setting value to 0");
		try { device.setValue(0); } catch (RuntimeIOException e) { }
		try {
			device.close();
		} catch (IOException e) {
			// Log and ignore
			Logger.warn(e, "Error closing device: {}", e);
		}
		Logger.debug("device closed");
	}
	
	protected void onOffLoop(float onTime, float offTime, int n, boolean background) throws RuntimeIOException {
		stopLoops();
		if (background) {
			DioZeroScheduler.getDaemonInstance().execute(() -> {
				backgroundThread = Thread.currentThread();
				onOffLoop(onTime, offTime, n);
				Logger.info("Background on-off loop finished");
				backgroundThread = null;
			});
		} else {
			onOffLoop(onTime, offTime, n);
		}
	}
	
	private void onOffLoop(float onTime, float offTime, int n) throws RuntimeIOException {
		if (n > 0) {
			running = true;
			for (int i=0; i<n && running; i++) {
				onOff(onTime, offTime);
			}
			running = false;
		} else if (n == INFINITE_ITERATIONS) {
			running = true;
			while (running) {
				onOff(onTime, offTime);
			}
		}
	}
	
	protected void fadeInOutLoop(float fadeTime, int steps, int iterations, boolean background) throws RuntimeIOException {
		stopLoops();
		if (background) {
			DioZeroScheduler.getDaemonInstance().execute(() -> {
				backgroundThread = Thread.currentThread();
				fadeInOutLoop(fadeTime, steps, iterations);
				Logger.info("Background fade in-out loop finished");
				backgroundThread = null;
			});
		} else {
			fadeInOutLoop(fadeTime, steps, iterations);
		}
	}

	private void fadeInOutLoop(float fadeTime, int steps, int iterations) throws RuntimeIOException {
		float sleep_time = fadeTime / steps;
		float delta = 1f / steps;
		if (iterations > 0) {
			running = true;
			for (int i=0; i<iterations && running; i++) {
				fadeInOut(sleep_time, delta);
			}
			running = false;
		} else if (iterations == INFINITE_ITERATIONS) {
			running = true;
			while (running) {
				fadeInOut(sleep_time, delta);
			}
		}
	}
	
	private void fadeInOut(float sleepTime, float delta) throws RuntimeIOException {
		float value = 0;
		while (value <= 1 && running) {
			setValueInternal(value);
			SleepUtil.sleepSeconds(sleepTime);
			value += delta;
		}
		value = 1;
		while (value >= 0 && running) {
			setValueInternal(value);
			SleepUtil.sleepSeconds(sleepTime);
			value -= delta;
		}
	}
	
	private void stopLoops() {
		running = false;
	}

	private void onOff(float onTime, float offTime) throws RuntimeIOException {
		setValueInternal(1);
		SleepUtil.sleepSeconds(onTime);
		setValueInternal(0);
		SleepUtil.sleepSeconds(offTime);
	}

	private void setValueInternal(float value) throws RuntimeIOException {
		device.setValue(value);
	}
	
	// Exposed operations
	public float getValue() throws RuntimeIOException {
		return device.getValue();
	}

	public void setValue(float value) throws RuntimeIOException {
		stopLoops();
		setValueInternal(value);
	}
	
	public void on() throws RuntimeIOException {
		stopLoops();
		setValueInternal(1);
	}
	
	public void off() throws RuntimeIOException {
		stopLoops();
		setValueInternal(0);
	}
	
	public void toggle() throws RuntimeIOException {
		stopLoops();
		setValueInternal(1 - device.getValue());
	}
	
	public boolean isOn() throws RuntimeIOException {
		return device.getValue() > 0;
	}
}
