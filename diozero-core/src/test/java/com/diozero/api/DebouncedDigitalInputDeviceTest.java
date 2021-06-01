package com.diozero.api;

import java.util.concurrent.Executors;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DebouncedDigitalInputDeviceTest.java
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

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.util.SleepUtil;

public class DebouncedDigitalInputDeviceTest implements DeviceEventConsumer<DigitalInputEvent> {
	private static ScheduledExecutorService executor;

	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setDigitalInputDeviceClass(TestDigitalInputDevice.class);
		executor = Executors.newScheduledThreadPool(1);
	}

	@AfterAll
	public static void afterAll() {
		executor.shutdown();
	}

	private int eventCount;

	@BeforeEach
	public void beforeEach() {
		eventCount = 0;
	}

	@Test
	public void testNoEvents() {
		System.out.println("testNoEvents() - Start");

		int gpio = 0;
		int debounce_time_ms = 200;
		int delay_secs = 5;
		try (final DebouncedDigitalInputDevice ddid = new DebouncedDigitalInputDevice(gpio, debounce_time_ms)) {
			ddid.addListener(this);

			Runnable event_generator = new Runnable() {
				boolean value = true;

				@Override
				public void run() {
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), value));
					value = !value;
				}
			};

			// Make sure that the debounce thread is out of alignment with the event
			// generation thread
			SleepUtil.sleepMillis(debounce_time_ms / 4);

			// Generate 1 event twice within the debounce time, toggling the value each time
			ScheduledFuture<?> future = executor.scheduleAtFixedRate(event_generator, 0, debounce_time_ms / 2,
					TimeUnit.MILLISECONDS);

			// Sleep for delay_secs
			SleepUtil.sleepSeconds(delay_secs);

			future.cancel(true);

			// Should generate no events as they are being triggered more frequently than
			// the debounce time
			Assertions.assertEquals(0, eventCount);
		}

		System.out.println("testNoEvents() - End");
	}

	@Test
	public void testOneEventPerSecond() {
		System.out.println("testOneEventPerSecond() - Start");

		int gpio = 0;
		int debounce_time_ms = 500;
		int sleep_secs = 5;
		try (final DebouncedDigitalInputDevice ddid = new DebouncedDigitalInputDevice(gpio, debounce_time_ms)) {
			ddid.addListener(this);

			Runnable event_generator = new Runnable() {
				boolean value = true;

				@Override
				public void run() {
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), value));
					value = !value;
				}
			};

			// Make sure that the debounce thread is out of alignment with the event
			// generation thread
			SleepUtil.sleepMillis(50);

			// Generate events that are held for twice the debounce time
			ScheduledFuture<?> future = executor.scheduleAtFixedRate(event_generator, 0, debounce_time_ms * 2,
					TimeUnit.MILLISECONDS);

			// Sleep for 5s
			SleepUtil.sleepSeconds(sleep_secs);

			future.cancel(true);

			// There should have been 1 event every second (there's actually sleep_secs -
			// 1) due to the slight delay starting the event generator thread...
			Assertions.assertTrue(Math.abs(eventCount - sleep_secs) <= 1);
		}

		System.out.println("testOneEventPerSecond() - End, eventCount: " + eventCount);
	}

	@Test
	public void testSimilarPeriod() {
		System.out.println("testSimilarPeriod() - Start");

		int gpio = 0;
		int debounce_time_ms = 200;
		int event_hold_ms = debounce_time_ms + 10;
		int sleep_secs = 5;
		try (final DebouncedDigitalInputDevice ddid = new DebouncedDigitalInputDevice(gpio, debounce_time_ms)) {
			ddid.addListener(this);

			Runnable event_generator = new Runnable() {
				boolean value = true;

				@Override
				public void run() {
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), value));
					value = !value;
				}
			};

			// Make sure that the debounce thread is out of alignment with the event
			// generation thread
			SleepUtil.sleepMillis(debounce_time_ms / 2);

			// Generate events that are held for just over the debounce time
			ScheduledFuture<?> future = executor.scheduleAtFixedRate(event_generator, 0, event_hold_ms,
					TimeUnit.MILLISECONDS);

			// Sleep for 5s
			SleepUtil.sleepSeconds(sleep_secs);

			future.cancel(true);

			// There should have been 5 events every 1.05 seconds
			Assertions.assertTrue(Math.abs(eventCount - sleep_secs * 1_000 / event_hold_ms) <= 1);
		}

		System.out.println("testOneEventPerSecond() - End");
	}

	@Test
	public void testSlowPeriod() {
		System.out.println("testSlowPeriod() - Start");

		int gpio = 0;
		int debounce_time_ms = 50;
		int event_hold_ms = 500;
		int sleep_secs = 5;
		try (final DebouncedDigitalInputDevice ddid = new DebouncedDigitalInputDevice(gpio, debounce_time_ms)) {
			ddid.addListener(this);

			Runnable event_generator = new Runnable() {
				boolean value = true;

				@Override
				public void run() {
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), value));
					value = !value;
				}
			};

			// Generate events that are held for just over the debounce time.
			// Make sure that the debounce thread is out of alignment with the event
			// generation thread.
			ScheduledFuture<?> future = executor.scheduleAtFixedRate(event_generator, debounce_time_ms / 2,
					event_hold_ms, TimeUnit.MILLISECONDS);

			// Sleep for 5s
			SleepUtil.sleepSeconds(sleep_secs);

			future.cancel(true);

			System.out.println(eventCount);

			// There should have been 2 events every second
			Assertions.assertEquals(sleep_secs * (1_000 / event_hold_ms), eventCount);
		}

		System.out.println("testOneEventPerSecond() - End");
	}

	@Test
	public void testIgnoreDuplicates() {
		System.out.println("testIgnoreDuplicates() - Start");

		int gpio = 0;
		int debounce_time_ms = 100;
		int sleep_secs = 1;
		try (final DebouncedDigitalInputDevice ddid = new DebouncedDigitalInputDevice(gpio, debounce_time_ms)) {
			ddid.addListener(this);

			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					long start_ms = System.currentTimeMillis();
					// Send true
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), true));
					SleepUtil.sleepMillis(10);
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), true));
					SleepUtil.sleepMillis(10);
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), true));

					// Wait at least debounce_time_ms so that the event fires
					SleepUtil.sleepMillis(Math.max(
							(debounce_time_ms + debounce_time_ms / 2) - (System.currentTimeMillis() - start_ms), 10));

					start_ms = System.currentTimeMillis();
					// Send false
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), false));
					SleepUtil.sleepMillis(10);
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), false));
					SleepUtil.sleepMillis(10);
					ddid.accept(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), false));

					// Wait at least debounce_time_ms so that the event fires
					SleepUtil.sleepMillis(Math.max(
							(debounce_time_ms + debounce_time_ms / 2) - (System.currentTimeMillis() - start_ms), 10));
				}
			};
			Future<?> future = executor.submit(event_generator);

			// Sleep for sleep_secs
			SleepUtil.sleepSeconds(sleep_secs);

			future.cancel(true);

			// There should have been 2 events...
			Assertions.assertEquals(2, eventCount);
		}

		System.out.println("testIgnoreDuplicates() - End");
	}

	@Test
	public void bouncySwitch1() {
		Logger.debug("bouncySwitch1() - Start");

		int gpio = 0;
		int debounce_time_ms = 30;
		int sleep_secs = 1;
		try (final DebouncedDigitalInputDevice ddid = new DebouncedDigitalInputDevice(gpio, debounce_time_ms)) {
			ddid.addListener(this);

			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					while (true) {
						// Low to high transition
						// This takes ~ 12ms on MacBook Pro 2019 Core i9
						for (int i = 0; i < 5; i++) {
							fastBlip(ddid, true);
						}
						SleepUtil.sleepMillis(50);

						// High to low transition
						for (int i = 0; i < 5; i++) {
							fastBlip(ddid, false);
						}
						SleepUtil.sleepMillis(50);

						// Total period is ~124ms
					}
				}
			};

			// Make sure that the debounce thread is out of alignment with the event
			// generation thread
			SleepUtil.sleepMillis(20);

			Future<?> future = executor.submit(event_generator);

			// Sleep for sleep_secs
			SleepUtil.sleepSeconds(sleep_secs);

			future.cancel(true);

			// Should be ~2 events every 125ms
			// Assertions.assertEquals((sleep_secs / 0.125f) * 2 - 1, eventCount);
		}

		Logger.debug("bouncySwitch1() - End");
	}

	static void fastBlip(DigitalInputDevice d, boolean finalValue) {
		d.accept(new DigitalInputEvent(d.getGpio(), System.currentTimeMillis(), System.nanoTime(), !finalValue));
		SleepUtil.sleepMillis(1);
		d.accept(new DigitalInputEvent(d.getGpio(), System.currentTimeMillis(), System.nanoTime(), finalValue));
		SleepUtil.sleepMillis(1);
	}

	@Override
	public void accept(DigitalInputEvent event) {
		Logger.debug("Event: {}", event);
		eventCount++;
	}
}
