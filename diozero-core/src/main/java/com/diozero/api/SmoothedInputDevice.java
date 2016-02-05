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


import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * 
 * TODO Not yet implemented
 *
 */
public class SmoothedInputDevice extends WaitableDigitalInputDevice {
	private static final Logger logger = LogManager.getLogger(SmoothedInputDevice.class);
	
	private float threshold;
	private GpioQueue queue;

	/**
	 * @param pinNumber
	 * @param pullUp
	 * @param threshold
	 * 				The value above which the device will be considered "on".
	 * @param queueLen
	 * 				The length of the internal queue which is filled by the background thread.
	 * @param sampleWait
	 * 				The length of time to wait between retrieving the state of the 
	 *				underlying device. Defaults to 0.0 indicating that values are retrieved 
	 *				as fast as possible. 
	 * @param partial
	 * 				If 'False' (the default), attempts to read the state of the device 
	 *				(from the 'is_active' property) will block until the queue has filled. 
	 *				If 'True', a value will be returned immediately, but be aware that this 
	 *				value is likely to fluctuate excessively. 
	 * @throws IOException
	 */
	public SmoothedInputDevice(int pinNumber, GpioPullUpDown pud, float threshold,
			int queueLen, float sampleWait, boolean partial) throws IOException {
		this(pinNumber, pud, threshold, queueLen, sampleWait, partial, GpioEventTrigger.BOTH);
	}
	
	public SmoothedInputDevice(int pinNumber, GpioPullUpDown pud, float threshold,
			int queueLen, float sampleWait, boolean partial, GpioEventTrigger trigger) throws IOException {
		super(pinNumber, pud, trigger);
		
		this.threshold = threshold;
		
		queue = new GpioQueue(this, queueLen, sampleWait, partial);
	}
	
	@Override
	public void close() {
		logger.debug("close()");
		queue.stop();
		super.close();
	}
	
	/**
	 * The length of the internal queue of values which is averaged to
	 * determine the overall state of the device. This defaults to '5'
	 */
	public int getQueueLen() {
		return queue.getMaxLength();
	}
	
	/**
	 * If 'False' (the default), attempts to read the 'value' or 'is_active'
	 * properties will block until the queue has filled.
	 */
	public boolean isPartial() {
		return queue.isPartial();
	}
	
	/**
	 * Returns the mean of the values in the internal queue. This is
	 * compared to 'threshold' to determine whether 'is_active' is `True'
	 */
	public float getFloatValue() {
		return queue.getValue();
	}
	
	/**
	 * If 'value' exceeds this amount, then 'is_active' will return 'True'
	 */
	public float getThreshold() {
		return threshold;
	}
	
	public void setThreshold(float threshold) {
		if (threshold <0 || threshold > 1) {
			throw new IllegalArgumentException("threshold must be between zero and one exclusive");
		}
		this.threshold = threshold;
	}

	/**
	 * Returns 'True' if the device is currently active and 'False' otherwise
	 */
	@Override
	public boolean isActive() {
		return getFloatValue() > threshold;
	}
}

class GpioQueue {
	private Deque<Event> queue;
	private float sampleWait;
	private boolean partial;
	private SmoothedInputDevice parent;
	private Event full;
	private int queueLen;
	
	public GpioQueue(SmoothedInputDevice parent, int queueLen, float sampleWait, boolean partial) {
		queue = new ArrayDeque<>(queueLen);
		
		this.queueLen = queueLen;
		this.parent = parent;
		this.sampleWait = sampleWait;
		this.partial = partial;
		full = new Event();
	}
	
	public boolean isPartial() {
		return partial;
	}

	public int getMaxLength() {
		return queueLen;
	}

	public void stop() {
		// TODO Auto-generated method stub
	}

	public float getValue() {
		if (! partial) {
			try {
				full.wait();
			} catch (InterruptedException ie) {
				
			}
		}
		
		if (queue.isEmpty()) {
			return 0;
		}
		
		return 0;//sum(queue) / queue.size();
	}
	
	public void fill() {
		/*
		while (not self.stopping.wait(self.sample_wait) and
				len(self.queue) < self.queue.maxlen):
			self.queue.append(self.parent._read())
			if self.partial:
				self.parent._fire_events()
		self.full.set()
		while not self.stopping.wait(self.sample_wait):
			self.queue.append(self.parent._read())
			self.parent._fire_events()
		*/
	}
}

class Event {
	
}
