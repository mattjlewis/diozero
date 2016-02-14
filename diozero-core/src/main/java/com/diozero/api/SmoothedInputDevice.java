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

import com.diozero.util.RuntimeIOException;

/**
 * Represents a generic input device which takes its value from the mean of a
 * queue of historical values.
 * 
 * This class extends 'WaitableInputDevice' with a queue which is filled by a
 * background thread which continually polls the state of the underlying
 * device. The mean of the values in the queue is compared to a threshold
 * which is used to determine the state of the 'is_active' property.
 * 
 * This class is intended for use with devices which either exhibit analogue
 * behaviour (such as the charging time of a capacitor with an LDR), or those
 * which exhibit "twitchy" behaviour (such as certain motion sensors).
 */
public class SmoothedInputDevice extends DigitalInputDevice {
	private int threshold;
	private int age;
	private Queue<Long> queue;

	/**
	 * @param pinNumber
	 * @param pullUp
	 * @param threshold
	 * 				The value above which the device will be considered "on".
	 * @param age
	 * 				The time in millis in which to keep items in the queue
	 * @throws RuntimeIOException
	 */
	public SmoothedInputDevice(int pinNumber, GpioPullUpDown pud, int threshold,
			int age) throws RuntimeIOException {
		this(pinNumber, pud, threshold, age, GpioEventTrigger.RISING);
	}
	
	public SmoothedInputDevice(int pinNumber, GpioPullUpDown pud, int threshold,
			int age, GpioEventTrigger trigger) throws RuntimeIOException {
		super(pinNumber, pud, trigger);
		
		this.threshold = threshold;
		this.age = age;
		
		queue = new LinkedList<>();
		GpioScheduler.getInstance().scheduleAtFixedRate(new EventDetection(), age, age, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * If the number of on events exceeds this amount, then 'is_active' will return 'True'
	 */
	public int getThreshold() {
		return threshold;
	}
	
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}


	@Override
	public void valueChanged(DigitalPinEvent event) {
		if (event.getValue()) {
			synchronized (queue) {
				queue.add(Long.valueOf(event.getEpochTime()));
			}
		}
	}
	
	private class EventDetection implements Runnable {
		private Predicate<Long> predicate;
		private long now;
		
		EventDetection() {
			predicate = time -> time.longValue() < (now - age);
		}
		
		@Override
		public void run() {
			long nano_time = System.nanoTime();
			now = System.currentTimeMillis();
			// Purge any old events
			synchronized (queue) {
				queue.removeIf(predicate);
				// Check if the number of events exceeds the threshold
				if (queue.size() > threshold) {
					SmoothedInputDevice.super.valueChanged(new DigitalPinEvent(pinNumber, now, nano_time, true));
					// If an event is fired clear the queue of all events
					queue.clear();
				}
			}
		}
	}
}
