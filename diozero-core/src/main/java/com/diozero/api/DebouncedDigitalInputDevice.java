package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DebouncedDigitalInputDevice.java
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * Digital input device with debounce logic. The goal of this debounce
 * implementation is to only detect level changes that are held for the
 * specified debounce time. All other level changes that are shorter than that
 * duration will be ignored.
 */
public class DebouncedDigitalInputDevice extends DigitalInputDevice {
	private int debounceTimeMs;
	private Queue<DigitalInputEvent> eventQueue;
	private Future<?> changeDetectionFuture;
	private AtomicBoolean running;
	private boolean lastReportedValue;
	private boolean nextValue;
	private long changeTimeMs;
	private long changeTimeNs;

	/**
	 * @param gpio           GPIO
	 * @param debounceTimeMs Specifies the length of time (in seconds) that the
	 *                       component will ignore changes in state after an initial
	 *                       change.
	 * @throws RuntimeIOException       if an I/O error occurs
	 * @throws IllegalArgumentException if the debounce time is less than 0
	 */
	public DebouncedDigitalInputDevice(int gpio, int debounceTimeMs)
			throws RuntimeIOException, IllegalArgumentException {
		this(gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH, debounceTimeMs);
	}

	/**
	 * @param gpio           GPIO
	 * @param pud            Pull-up/down configuration
	 * @param debounceTimeMs Specifies the length of time (in seconds) that the
	 *                       component will ignore changes in state after an initial
	 *                       change.
	 * @throws RuntimeIOException       if an I/O error occurs
	 * @throws IllegalArgumentException if the debounce time is less than 0
	 */
	public DebouncedDigitalInputDevice(int gpio, GpioPullUpDown pud, int debounceTimeMs)
			throws RuntimeIOException, IllegalArgumentException {
		this(gpio, pud, GpioEventTrigger.BOTH, debounceTimeMs);
	}

	private DebouncedDigitalInputDevice(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger, int debounceTimeMs)
			throws RuntimeIOException, IllegalArgumentException {
		super(gpio, pud, trigger);

		if (debounceTimeMs <= 0) {
			throw new IllegalArgumentException("Debounce time must be > 0");
		}

		this.debounceTimeMs = debounceTimeMs;

		// Initialise nextValue and lastReportedValue to the current value
		nextValue = lastReportedValue = getValue();
		changeTimeMs = System.currentTimeMillis();
		changeTimeNs = System.nanoTime();

		eventQueue = new ConcurrentLinkedQueue<>();
		running = new AtomicBoolean(true);
		changeDetectionFuture = DiozeroScheduler.getNonDaemonInstance().submit(this::changeDetection);
	}

	@Override
	public void accept(DigitalInputEvent event) {
		eventQueue.add(event);
	}

	@Override
	public void close() {
		Logger.trace("close()");
		if (running.getAndSet(false)) {
			changeDetectionFuture.cancel(true);
			try {
				changeDetectionFuture.get(1, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				// Ignore
			}
		}
		super.close();
	}

	/*-
	 *               +--------------+              +--------------+              +--------------+              +------
	 *               |              |              |              |              |              |              |
	 * --------------+              +--------------+              +--------------+              +--------------+
	 * |---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|
	 * t=0       t=10      t=20      t=30      t=40      t=50      t=60      t=70      t=80      t=90      t=100
	 * eT=0      eT=0      eT=14     eT=29
	 * eV=0      eV=       eV=1      eV=0
	 * nV=0      nV=0      nV=1      nV=1
	 * lV=0      lV=0      lV=0      lV=0
	 */
	public void changeDetection() {
		long start_ms;
		while (running.get()) {
			start_ms = System.currentTimeMillis();

			// Process all events in eventQueue
			// No need to synchronise as we're using a ConcurrentLinkedQueue

			// Process all events on the queue
			DigitalInputEvent event = eventQueue.poll();
			while (event != null) {
				// Check if the previous event was held for debounceTimeMs.
				// This can happen if an event occurred mid-sleep hence isn't caught by the
				// check at the end of the event processing loop.
				if (nextValue != lastReportedValue && (event.getEpochTime() - changeTimeMs) >= debounceTimeMs) {
					super.accept(new DigitalInputEvent(getGpio(), changeTimeMs, changeTimeNs, nextValue));
					lastReportedValue = nextValue;
					changeTimeMs = event.getEpochTime();
				}

				boolean value = event.getValue();

				// Record the time of change if there is a difference between event value and
				// the next value to be reported.
				// Note that you sometimes get repeat events for the same value.
				if (value != nextValue) {
					changeTimeMs = event.getEpochTime();
					changeTimeNs = event.getNanoTime();
					nextValue = value;
				}

				// Get the next event (if there is one)
				event = eventQueue.poll();
			}

			// Only fire an event when there has been a change to the last reported value
			// that has been held for at least debounceTimeMs
			if (nextValue != lastReportedValue) {
				// Note that an event might have arrived during the sleep period
				long diff_ms = System.currentTimeMillis() - changeTimeMs;
				/*-
				Logger.debug("nextValue ({}) != lastReportedValue ({}), diff_ms: {}, debounceTimeMs: {}",
						Boolean.valueOf(nextValue), Boolean.valueOf(lastReportedValue), Long.valueOf(diff_ms),
						Long.valueOf(debounceTimeMs));
						*/
				if (diff_ms >= debounceTimeMs) {
					// FIXME This calls out to user code in the same thread hence can delay start of
					// the next loop
					super.accept(new DigitalInputEvent(getGpio(), changeTimeMs, changeTimeNs, nextValue));
					lastReportedValue = nextValue;
					// Reset the changeTimeMs value so we don't send this event again
					changeTimeMs = System.currentTimeMillis();
					changeTimeNs = System.nanoTime();
				}
			}

			// Sleep for debounceTimeMs - the time taken to process events
			long sleep_ms = debounceTimeMs - (System.currentTimeMillis() - start_ms);
			if (sleep_ms > 0) {
				SleepUtil.sleepMillis(sleep_ms);
			} else if (Logger.isDebugEnabled()) {
				Logger.debug("Not sleeping - sleep_ms: {}", Long.valueOf(sleep_ms));
			}
		}

		Logger.debug("Exiting run loop");
	}
}
