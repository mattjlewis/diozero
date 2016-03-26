package com.diozero.api;

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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;

/**
 * Represents a generic input device which takes its value from the number of active
 * events over a specific time period.
 * 
 * This class extends {@link DigitalInputDevice} with a queue which is added to whenever
 * the input device is active. The number of the active events in the queue is compared
 * to a threshold which is used to determine the state of the 'active' property.
 * 
 * Any active events over the specified eventAge are removed by a background thread.
 * 
 * This class is intended for use with devices which exhibit "twitchy" behaviour (such
 * as certain motion sensors).
 */
public class SmoothedInputDevice extends WaitableDigitalInputDevice {
	private int threshold;
	private int eventAge;
	private int eventDetectPeriod;
	private Queue<Long> queue;

	/**
	 * @param pinNumber GPIO pin number
	 * @param pud Pull-up/down configuration
	 * @param threshold
	 * 				The value above which the device will be considered "on".
	 * @param eventAge
	 * 				The time in milliseconds to keep active events in the queue.
	 * @param eventDetectPeriod
	 * 				How frequently to check for events.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public SmoothedInputDevice(int pinNumber, GpioPullUpDown pud, int threshold,
			int eventAge, int eventDetectPeriod) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, pud, threshold, eventAge, eventDetectPeriod);
	}
	
	public SmoothedInputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud, int threshold,
			int eventAge, int eventDetectPeriod) throws RuntimeIOException {
		super(deviceFactory, pinNumber, pud, pud == GpioPullUpDown.PULL_UP ? GpioEventTrigger.FALLING : GpioEventTrigger.RISING);
		
		this.threshold = threshold;
		this.eventAge = eventAge;
		this.eventDetectPeriod = eventDetectPeriod;
		
		queue = new LinkedList<>();
		DioZeroScheduler.getDaemonInstance().scheduleAtFixedRate(new EventDetection(),
				eventDetectPeriod, eventDetectPeriod, TimeUnit.MILLISECONDS);
	}

	@Override
	public void valueChanged(DigitalInputEvent event) {
		event.setActiveHigh(activeHigh);
		if (event.isActive()) {
			synchronized (queue) {
				queue.add(Long.valueOf(event.getEpochTime()));
			}
		}
	}
	
	private class EventDetection implements Runnable {
		private Predicate<Long> removePredicate;
		private long now;
		
		EventDetection() {
			removePredicate = time -> time.longValue() < (now - eventAge);
		}
		
		@Override
		public void run() {
			long nano_time = System.nanoTime();
			now = System.currentTimeMillis();
			synchronized (queue) {
				// Purge any old events
				queue.removeIf(removePredicate);
				
				// Check if the number of events exceeds the threshold
				if (queue.size() > threshold) {
					SmoothedInputDevice.super.valueChanged(new DigitalInputEvent(pinNumber, now, nano_time, true));
					
					// If an event is fired clear the queue of all events
					queue.clear();
				}
			}
		}
	}
	
	/**
	 * If the number of on events younger than eventAge exceeds this amount, then 'isActive' will return 'True'.
	 * @return event threshold
	 */
	public int getThreshold() {
		return threshold;
	}
	
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
	
	public int getEventAge() {
		return eventAge;
	}
	
	public void setEventAge(int eventAge) {
		this.eventAge = eventAge;
	}
	
	public int getEventDetectPeriod() {
		return eventDetectPeriod;
	}
}
