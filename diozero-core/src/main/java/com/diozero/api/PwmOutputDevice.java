package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PwmOutputDevice.java
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * Provide generic
 * <a href="https://en.wikipedia.org/wiki/Pulse-width_modulation">Pulse Width
 * Modulation (PWM)</a> output control.
 */
public class PwmOutputDevice extends GpioDevice {
	private static final int DEFAULT_PWM_FREQUENCY = 50;
	public static final int INFINITE_ITERATIONS = -1;

	private InternalPwmOutputDeviceInterface delegate;
	private AtomicBoolean running;
	private Thread backgroundThread;

	/**
	 * @param gpio GPIO to which the output device is connected.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public PwmOutputDevice(int gpio) throws RuntimeIOException {
		this(gpio, 0);
	}

	/**
	 * @param gpio         GPIO to which the output device is connected.
	 * @param initialValue Initial output value (0..1).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public PwmOutputDevice(int gpio, float initialValue) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialValue);
	}

	/**
	 * @param gpio         GPIO to which the output device is connected.
	 * @param initialValue Initial output value (0..1).
	 * @param pwmFrequency PWM frequency (Hz).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public PwmOutputDevice(int gpio, int pwmFrequency, float initialValue) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pwmFrequency, initialValue);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this device.
	 * @param gpio          GPIO to which the output device is connected.
	 * @param initialValue  Initial output value (0..1).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public PwmOutputDevice(PwmOutputDeviceFactoryInterface deviceFactory, int gpio, float initialValue)
			throws RuntimeIOException {
		this(deviceFactory, gpio, DEFAULT_PWM_FREQUENCY, initialValue);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this device.
	 * @param pwmOrGpioNum  GPIO to which the output device is connected.
	 * @param pwmFrequency  PWM frequency (Hz).
	 * @param initialValue  Initial output value (0..1).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public PwmOutputDevice(PwmOutputDeviceFactoryInterface deviceFactory, int pwmOrGpioNum, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		this(deviceFactory, deviceFactory.getBoardPinInfo().getByPwmOrGpioNumberOrThrow(pwmOrGpioNum), pwmFrequency,
				initialValue);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this device.
	 * @param pinInfo       GPIO to which the output device is connected.
	 * @param pwmFrequency  PWM frequency (Hz).
	 * @param initialValue  Initial output value (0..1).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public PwmOutputDevice(PwmOutputDeviceFactoryInterface deviceFactory, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		super(pinInfo);

		running = new AtomicBoolean();
		this.delegate = deviceFactory.provisionPwmOutputDevice(pinInfo, pwmFrequency, initialValue);
	}

	@Override
	public void close() {
		Logger.trace("close()");
		if (running.get()) {
			stopLoops();
			Logger.debug("Interrupting background thread " + backgroundThread.getName());
			backgroundThread.interrupt();
		}
		Logger.trace("Setting value to 0");
		try {
			delegate.setValue(0);
		} catch (RuntimeIOException e) {
			// Ignore
		}
		delegate.close();
		Logger.trace("device closed");
	}

	protected void onOffLoop(float onTime, float offTime, int n, boolean background) throws RuntimeIOException {
		stopLoops();
		if (background) {
			DiozeroScheduler.getNonDaemonInstance().execute(() -> {
				backgroundThread = Thread.currentThread();
				onOffLoop(onTime, offTime, n);
				Logger.debug("Background on-off loop finished");
			});
		} else {
			onOffLoop(onTime, offTime, n);
		}
	}

	private void onOffLoop(float onTime, float offTime, int n) throws RuntimeIOException {
		if (n > 0) {
			running.getAndSet(true);
			for (int i = 0; i < n && running.get(); i++) {
				onOff(onTime, offTime);
			}
			running.getAndSet(false);
		} else if (n == INFINITE_ITERATIONS) {
			running.getAndSet(true);
			while (running.get()) {
				onOff(onTime, offTime);
			}
		}
	}

	protected void fadeInOutLoop(float fadeTime, int steps, int iterations, boolean background)
			throws RuntimeIOException {
		stopLoops();
		if (background) {
			DiozeroScheduler.getNonDaemonInstance().execute(() -> {
				backgroundThread = Thread.currentThread();
				fadeInOutLoop(fadeTime, steps, iterations);
				Logger.debug("Background fade in-out loop finished");
			});
		} else {
			fadeInOutLoop(fadeTime, steps, iterations);
		}
	}

	private void fadeInOutLoop(float fadeTime, int steps, int iterations) throws RuntimeIOException {
		float sleep_time = fadeTime / steps;
		float delta = 1f / steps;
		Logger.debug("fadeTime={}, steps={}, sleep_time={}s, delta={}", Float.valueOf(fadeTime), Integer.valueOf(steps),
				Float.valueOf(sleep_time), Float.valueOf(delta));
		running.getAndSet(true);
		if (iterations > 0) {
			for (int i = 0; i < iterations && running.get(); i++) {
				fadeInOut(sleep_time, delta);
			}
			running.getAndSet(false);
		} else if (iterations == INFINITE_ITERATIONS) {
			while (running.get()) {
				fadeInOut(sleep_time, delta);
			}
		}
	}

	private void fadeInOut(float sleepTime, float delta) throws RuntimeIOException {
		float value = 0;
		while (value <= 1 && running.get()) {
			setValueInternal(value);
			SleepUtil.sleepSeconds(sleepTime);
			value += delta;
		}
		value = 1;
		while (value >= 0 && running.get()) {
			setValueInternal(value);
			SleepUtil.sleepSeconds(sleepTime);
			value -= delta;
		}
	}

	private void stopLoops() {
		running.getAndSet(false);
	}

	private void onOff(float onTime, float offTime) throws RuntimeIOException {
		setValueInternal(1);
		SleepUtil.sleepSeconds(onTime);
		setValueInternal(0);
		SleepUtil.sleepSeconds(offTime);
	}

	protected void setValueInternal(float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("Value must be 0..1, you requested " + value);
		}
		delegate.setValue(value);
	}

	// Exposed operations

	/**
	 * Get the current PWM output value (0..1).
	 *
	 * @return Current PWM output value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public float getValue() throws RuntimeIOException {
		return delegate.getValue();
	}

	/**
	 * Set the PWM output value (0..1).
	 *
	 * @param value New PWM output value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void setValue(float value) throws RuntimeIOException {
		stopLoops();
		setValueInternal(value);
	}

	public int getPwmFrequency() {
		return delegate.getPwmFrequency();
	}

	public void setPwmFrequency(int pwmFrequency) throws RuntimeIOException {
		delegate.setPwmFrequency(pwmFrequency);
	}

	/**
	 * Turn on the device (same as {@code setValue(1)}).
	 *
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void on() throws RuntimeIOException {
		stopLoops();
		setValueInternal(1);
	}

	/**
	 * Turn off the device (same as {@code setValue(0)}).
	 *
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void off() throws RuntimeIOException {
		stopLoops();
		setValueInternal(0);
	}

	/**
	 * Toggle the state of the device (same as {@code setValue(1 - getvalue())} ).
	 *
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void toggle() throws RuntimeIOException {
		stopLoops();
		setValueInternal(1 - getValue());
	}

	/**
	 * Get the device on / off status.
	 *
	 * @return Returns true if the device currently has a value &gt; 0.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public boolean isOn() throws RuntimeIOException {
		return delegate.getValue() > 0;
	}
}
