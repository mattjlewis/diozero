package com.diozero;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SmoothedInputTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InputEventListener;
import com.diozero.api.SmoothedInputDevice;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.internal.provider.test.TestDigitalOutputDevice;
import com.diozero.util.SleepUtil;

public class SmoothedInputTest implements InputEventListener<DigitalInputEvent> {
	@BeforeClass
	public static void beforeClass() {
		TestDeviceFactory.setDigitalInputDeviceClass(TestDigitalInputDevice.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(TestDigitalOutputDevice.class);
	}
	
	@Test
	public void test() {
		int pin = 1;
		float delay = 5;
		// Require 10 events in 2 seconds to be considered on, check every 50ms
		try (SmoothedInputDevice device = new SmoothedInputDevice(pin, GpioPullUpDown.NONE, 10, 2000, 50)) {
			device.addListener(this);
			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					long nano_time = System.nanoTime();
					long now = System.currentTimeMillis();
					device.valueChanged(new DigitalInputEvent(pin, now, nano_time, true));
				}
			};
			
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
			
			// Generate 1 event every 100ms -> 10 events per second, therefore should get a smoothed event every 1s
			ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(event_generator, 100, 100, TimeUnit.MILLISECONDS);
			Logger.debug("Sleeping for {}s", Float.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			
			Logger.debug("Stopping event generation and sleeping for {}s", Float.valueOf(delay));
			future.cancel(true);
			SleepUtil.sleepSeconds(delay);
			
			// Generate 1 event every 50ms -> 20 events per second
			future = scheduler.scheduleAtFixedRate(event_generator, 100, 100, TimeUnit.MILLISECONDS);
			Logger.debug("Restarting event generation and sleeping for {}s", Float.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			future.cancel(true);
			
			scheduler.shutdownNow();
		} catch (Throwable t) {
			Logger.error(t, "Error: {}", t);
		}
	}

	@Override
	public void valueChanged(DigitalInputEvent event) {
		Logger.debug("valueChanged({})", event);
	}
}
