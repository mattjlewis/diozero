package com.diozero.util;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.*;

public class SoftwarePwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface, Runnable {
	static {
		LibraryLoader.loadLibrary(SleepUtil.class, "diozero-system-utils");
	}
	
	private GpioDigitalOutputDeviceInterface digitalOutputDevice;
	private ScheduledFuture<?> future;
	private int periodMs;
	private AtomicInteger dutyNs;
	private AtomicBoolean fullyOn;
	private AtomicBoolean fullyOff;
	
	public SoftwarePwmOutputDevice(String key, DeviceFactoryInterface deviceFactory,
			GpioDigitalOutputDeviceInterface digitalOutputDevice, int frequency, float initialValue) {
		super(key, deviceFactory);
		
		this.digitalOutputDevice = digitalOutputDevice;
		dutyNs = new AtomicInteger();
		fullyOn = new AtomicBoolean();
		fullyOff = new AtomicBoolean();
		
		periodMs = 1_000 / frequency;
		setValue(initialValue);
	}
	
	public void start() {
		future = DioZeroScheduler.getNonDaemonInstance().scheduleAtFixedRate(this, periodMs, periodMs, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
		if (future != null) {
			future.cancel(true);
		}
	}

	@Override
	public void run() {
		if (fullyOff.get()) {
			// Fully off
			digitalOutputDevice.setValue(false);
		} else if (fullyOn.get()) {
			digitalOutputDevice.setValue(true);
		} else {
			digitalOutputDevice.setValue(true);
			SleepUtil.sleepNanos(dutyNs.get());
			digitalOutputDevice.setValue(false);
		}
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice() {}", getKey());
		stop();
		if (digitalOutputDevice != null) {
			digitalOutputDevice.close();
			digitalOutputDevice = null;
		}
	}
	
	@Override
	public float getValue() {
		return dutyNs.get() / (float) TimeUnit.MILLISECONDS.toNanos(periodMs);
	}
	
	@Override
	public void setValue(float value) {
		// Constrain to 0..1
		value = RangeUtil.constrain(value, 0, 1);
		dutyNs.set((int) (value * TimeUnit.MILLISECONDS.toNanos(periodMs)));
		if (value == 0) {
			fullyOn.set(false);
			fullyOff.set(true);
		} else if (value == 1) {
			fullyOn.set(true);
			fullyOff.set(false);
		} else {
			fullyOn.set(false);
			fullyOff.set(false);
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
}
