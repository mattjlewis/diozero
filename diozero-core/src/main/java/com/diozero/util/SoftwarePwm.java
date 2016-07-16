package com.diozero.util;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;

public class SoftwarePwm implements Runnable, Closeable {
	static {
		LibraryLoader.loadLibrary(SleepUtil.class, "diozero-system-utils");
	}
	
	private DigitalOutputDevice output;
	private ScheduledFuture<?> future;
	private int periodMs;
	private int dutyUs;
	
	public SoftwarePwm(int pinNumber, int frequency, int dutyUs) {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber);
		this.dutyUs = dutyUs;
		periodMs = 1_000 / frequency;
	}
	
	public SoftwarePwm(GpioDeviceFactoryInterface deviceFactory, int pinNumber) {
		this.output = new DigitalOutputDevice(deviceFactory, pinNumber, true, false);
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
		output.on();
		SleepUtil.sleepNanos(0, TimeUnit.MICROSECONDS.toNanos(dutyUs));
		output.off();
	}

	@Override
	public void close() {
		stop();
		if (output != null) {
			output.close();
		}
	}
}
