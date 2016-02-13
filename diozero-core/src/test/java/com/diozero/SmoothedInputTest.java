package com.diozero;

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


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.util.SleepUtil;

public class SmoothedInputTest implements InputEventListener<DigitalPinEvent> {
	@Test
	public void test() {
		int pin = 1;
		float delay = 2;
		// 10 events in 1 second
		try (SmoothedInputDevice device = new SmoothedInputDevice(pin, GpioPullUpDown.NONE, 10, 1000)) {
			device.addListener(this);
			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					long nano_time = System.nanoTime();
					long now = System.currentTimeMillis();
					device.valueChanged(new DigitalPinEvent(pin, now, nano_time, true));
				}
			};
			
			// Generate 1 event every 50ms -> 20 events per second
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(event_generator, 50, 50, TimeUnit.MILLISECONDS);
			Logger.debug("Sleeping for {}s", Float.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			
			Logger.debug("Stopping event generation and sleeping for {}s", Float.valueOf(delay));
			scheduler.shutdown();
			SleepUtil.sleepSeconds(delay);
			
			// Generate 1 event every 50ms -> 20 events per second
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(event_generator, 50, 50, TimeUnit.MILLISECONDS);
			Logger.debug("Restarting event generation and sleeping for {}s", Float.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			scheduler.shutdown();
		}
	}

	@Override
	public void valueChanged(DigitalPinEvent event) {
		Logger.debug("valueChanged({})", event);
	}
}
