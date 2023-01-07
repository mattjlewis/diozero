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
import java.util.concurrent.locks.LockSupport;

import org.tinylog.Logger;

import com.diozero.internal.spi.*;
import com.diozero.util.DiozeroScheduler;

/**
 * Generate a very poor approximation of a PWM signal - use at your own risk!
 * All timing is in milliseconds hence it is strongly recommend to use a
 * frequency of 50Hz to minimise integer rounding errors.
 */
public class SoftwarePwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface {
	private final GpioDigitalOutputDeviceInterface digitalOutputDevice;
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final AtomicInteger periodNs = new AtomicInteger();
	private final AtomicInteger dutyNs = new AtomicInteger();
	private Future<?> future;

	public SoftwarePwmOutputDevice(String key, DeviceFactoryInterface deviceFactory,
			GpioDigitalOutputDeviceInterface digitalOutputDevice, int frequencyHz, float initialValue) {
		super(key, deviceFactory);

		Logger.info("Hardware PWM not available for device {}, reverting to software", key);

		this.digitalOutputDevice = digitalOutputDevice;
		digitalOutputDevice.setChild(true);

		periodNs.set(Math.round(1_000_000_000f / frequencyHz));
		setValue(initialValue);
		start();
	}

	public void start() {
		if (!running.getAndSet(true)) {
			future = DiozeroScheduler.getNonDaemonInstance().submit(this::dutyLoop);
		}
	}

	public void stop() {
		if (running.getAndSet(false)) {
			if (future == null) {
				Logger.warn("Unexpected condition - future was null when stopping PWM output");
			} else {
				// Wait for the runnable to complete
				try {
					// Give it the period plus an additional 10ms grace to stop
					future.get(periodNs.get() + 10_000_000, TimeUnit.NANOSECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					Logger.debug(e, "Error waiting for future to complete: {}", e);
					// Cancel the future if it doesn't complete normally by setting running to false
					future.cancel(true);
				}
				future = null;
			}
		}
	}

	private void dutyLoop() {
		long start_ns;
		int lastDuty = Integer.MAX_VALUE;
		int lastPeriod = Integer.MAX_VALUE;

		while (running.get()) {
			start_ns = System.nanoTime();

			// so the value doesn't change mid-iteration
			int dutyDuty = dutyNs.get();
			int periodPeriod = periodNs.get();

			// if there's no change since the last iteration, don't do anything
			if (dutyDuty != lastDuty || lastPeriod != periodPeriod) {
				lastPeriod = periodPeriod;
				lastDuty = dutyDuty;

				if (dutyDuty == 0) {
					// Fully off
					digitalOutputDevice.setValue(false);
				}
				else if (dutyDuty == periodPeriod) {
					digitalOutputDevice.setValue(true);
				}
				else {
					digitalOutputDevice.setValue(true);
					// SleepUtil.sleepMillis(dutyMs.get());
					LockSupport.parkNanos(dutyDuty);
					digitalOutputDevice.setValue(false);
				}
			}
			// SleepUtil.sleepMillis(Math.max(1, periodMs.get() -
			// (System.currentTimeMillis() - start)));
			// Minimum sleep time is 0.1ms
			LockSupport.parkNanos(Math.max(100_000, periodPeriod - (System.nanoTime() - start_ns)));
		}
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice() {}", getKey());
		stop();
		// The diozero shutdown handler closes devices in an arbitrary order
		if (digitalOutputDevice.isOpen()) {
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
		return dutyNs.get() / (float) periodNs.get();
	}

	@Override
	public void setValue(float value) {
		dutyNs.set((int) Math.floor(value * periodNs.get()));
	}

	@Override
	public int getPwmFrequency() {
		return 1_000_000_000 / periodNs.get();
	}

	@Override
	public void setPwmFrequency(int frequencyHz) {
		// Save the current value
		float current_value = getValue();
		periodNs.set(Math.round(1_000_000_000f / frequencyHz));
		// Restore the equivalent value
		dutyNs.set(Math.round(current_value * periodNs.get()));
	}
}
