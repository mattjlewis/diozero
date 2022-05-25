package com.diozero.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SoftwarePwmOutputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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
import java.util.concurrent.atomic.AtomicInteger;

import org.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * Generate a very poor approximation of a PWM signal - use at your own risk!
 * All timing is in milliseconds hence it is strongly recommend to use a
 * frequency of 50Hz to minimise integer rounding errors.
 */
public class SoftwarePwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface, Runnable {
	private GpioDigitalOutputDeviceInterface digitalOutputDevice;
	private AtomicBoolean running;
	private AtomicInteger periodMs;
	private AtomicInteger dutyMs;
	private Future<?> future;

	public SoftwarePwmOutputDevice(String key, DeviceFactoryInterface deviceFactory,
			GpioDigitalOutputDeviceInterface digitalOutputDevice, int frequencyHz, float initialValue) {
		super(key, deviceFactory);

		Logger.info("Hardware PWM not available for device {}, reverting to software", key);

		this.digitalOutputDevice = digitalOutputDevice;
		digitalOutputDevice.setChild(true);
		running = new AtomicBoolean();

		periodMs = new AtomicInteger(Math.round(1_000f / frequencyHz));
		dutyMs = new AtomicInteger();
		setValue(initialValue);
		start();
	}

	public void start() {
		if (!running.getAndSet(true)) {
			future = DiozeroScheduler.getNonDaemonInstance().submit(this);
		}
	}

	public void stop() {
		if (running.get()) {
			running.set(false);

			if (future == null) {
				Logger.warn("Unexpected condition - future was null when stopping PWM output");
			} else {
				// Wait for the runnable to complete
				try {
					future.get(periodMs.get() + 10, TimeUnit.MILLISECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					Logger.debug(e, "Error waiting for future to complete: {}", e);
					// Cancel the future if it doesn't complete normally by setting running to false
					future.cancel(true);
				}
				future = null;
			}
		}
	}

	@Override
	public void run() {
		long start;
		while (running.get()) {
			start = System.currentTimeMillis();
			if (dutyMs.get() == 0) {
				// Fully off
				digitalOutputDevice.setValue(false);
			} else if (dutyMs == periodMs) {
				digitalOutputDevice.setValue(true);
			} else {
				digitalOutputDevice.setValue(true);
				SleepUtil.sleepMillis(dutyMs.get());
				digitalOutputDevice.setValue(false);
			}
			SleepUtil.sleepMillis(Math.max(1, periodMs.get() - (System.currentTimeMillis() - start)));
		}
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice() {}", getKey());
		stop();
		// The diozero shutdown handler closes devices in an arbitrary order
		if (digitalOutputDevice != null && digitalOutputDevice.isOpen()) {
			digitalOutputDevice.close();
		}
	}

	@Override
	public int getGpio() {
		return digitalOutputDevice.getGpio();
	}

	@Override
	public int getPwmNum() {
		return digitalOutputDevice.getGpio();
	}

	@Override
	public float getValue() {
		return dutyMs.get() / (float) periodMs.get();
	}

	@Override
	public void setValue(float value) {
		dutyMs.set((int) Math.floor(value * periodMs.get()));
	}

	@Override
	public int getPwmFrequency() {
		return 1_000 / periodMs.get();
	}

	@Override
	public void setPwmFrequency(int frequencyHz) {
		// Save the current value
		float current_value = getValue();
		periodMs.set(Math.round(1_000f / frequencyHz));
		// Restore the equivalent value
		dutyMs.set(Math.round(current_value * periodMs.get()));
	}
}
