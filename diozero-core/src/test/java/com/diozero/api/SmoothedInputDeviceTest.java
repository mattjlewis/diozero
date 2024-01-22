package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SmoothedInputDeviceTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.internal.provider.test.TestDigitalOutputDevice;
import com.diozero.util.SleepUtil;

public class SmoothedInputDeviceTest implements DeviceEventConsumer<DigitalInputEvent> {
	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setDigitalInputDeviceClass(TestDigitalInputDevice.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(TestDigitalOutputDevice.class);
	}

	private int eventCount;

	@Test
	public void testSmoothing() {
		eventCount = 0;
		int pin = 1;
		int delay_secs = 5;
		// Require 10 events in any 2 second period to be considered 'active', check
		// every 100ms
		try (SmoothedInputDevice device = new SmoothedInputDevice(pin, GpioPullUpDown.NONE, 10, 2000, 100)) {
			device.addListener(this);
			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					device.accept(new DigitalInputEvent(pin, System.currentTimeMillis(), System.nanoTime(), true));
				}
			};

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

			// Generate 1 event every 100ms -> 10 events per second, therefore should get a
			// smoothed event every 1s
			ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(event_generator, 100, 100, TimeUnit.MILLISECONDS);

			Logger.info("Sleeping for {}s", Integer.valueOf(delay_secs));
			SleepUtil.sleepSeconds(delay_secs);
			Logger.info("eventCount: {}, should be: {}", Integer.valueOf(eventCount), Integer.valueOf(delay_secs));
			Assertions.assertTrue(eventCount >= (delay_secs - 1) && eventCount <= (delay_secs + 1));

			Logger.info("Stopping event generation and sleeping for {}s", Integer.valueOf(delay_secs));
			future.cancel(true);
			SleepUtil.sleepSeconds(delay_secs);
			eventCount = 0;

			// Generate 1 event every 100ms -> 10 events per second
			future = scheduler.scheduleAtFixedRate(event_generator, 100, 100, TimeUnit.MILLISECONDS);
			Logger.info("Restarting event generation and sleeping for {}s", Integer.valueOf(delay_secs));
			SleepUtil.sleepSeconds(delay_secs);
			Assertions.assertTrue(eventCount >= (delay_secs - 1) && eventCount <= (delay_secs + 1));
			future.cancel(true);

			scheduler.shutdownNow();
		}
	}

	@Test
	public void testDebounce() {
		Logger.info("testDebounce() - start");

		eventCount = 0;
		int pin = 1;
		int delay_secs = 5;
		// Require 1 event in any 500ms period to be considered 'active', check every
		// 500ms
		try (SmoothedInputDevice device = SmoothedInputDevice.Builder.builder(pin).setThreshold(1).setEventAgeMs(500)
				.setEventDetectPeriodMs(500).build()) {
			device.addListener(this);
			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					device.accept(new DigitalInputEvent(pin, System.currentTimeMillis(), System.nanoTime(), true));
				}
			};

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

			// Generate 1 event every 250ms -> 4 events per second, should get 1 debounced
			// event every 500ms
			ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(event_generator, 250, 250, TimeUnit.MILLISECONDS);

			Logger.info("Sleeping for {}s", Integer.valueOf(delay_secs));
			SleepUtil.sleepSeconds(delay_secs);
			Logger.info("eventCount: {}, should be: {}", Integer.valueOf(eventCount), Integer.valueOf(2 * delay_secs));
			// Assertions.assertTrue(eventCount >= (delay_secs - 1) && eventCount <=
			// (delay_secs + 1));

			Logger.info("Stopping event generation and sleeping for {}s", Integer.valueOf(delay_secs));
			future.cancel(true);
			SleepUtil.sleepSeconds(delay_secs);
			eventCount = 0;

			// Generate 1 event every 250ms -> 4 events per second
			future = scheduler.scheduleAtFixedRate(event_generator, 250, 250, TimeUnit.MILLISECONDS);
			Logger.info("Restarting event generation and sleeping for {}s", Integer.valueOf(delay_secs));
			SleepUtil.sleepSeconds(delay_secs);
			// Assertions.assertTrue(eventCount >= (delay_secs - 1) && eventCount <=
			// (delay_secs + 1));
			future.cancel(true);

			scheduler.shutdownNow();
		}
		Logger.info("testDebounce() - end");
	}

	@Override
	public void accept(DigitalInputEvent event) {
		Logger.info("accept({})", event);
		if (event.isActive()) {
			eventCount++;
		}
	}
}
