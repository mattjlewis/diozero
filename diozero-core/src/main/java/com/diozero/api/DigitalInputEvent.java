package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     DigitalInputEvent.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

/**
 * Represents an input event from a digital device.
 */
public class DigitalInputEvent extends DeviceEvent {
	private boolean value;
	private boolean activeHigh;

	/**
	 * Constructs an input event from a digital device. See (@link
	 * com.diozero.api.DeviceEvent} for first 3 parameters.
	 * 
	 * @param gpio      GPIO number
	 * @param epochTime Unix epoch time: milliseconds elapsed since January 1, 1970
	 *                  (midnight UTC/GMT), not counting leap seconds
	 * @param nanoTime  The Java Virtual Machine's high-resolution time source, in
	 *                  nanoseconds (note this is unrelated to epochTime and uses
	 *                  CLOCK_MONOTONIC in the C clock_gettime() function)
	 * @param value     the event value
	 */
	public DigitalInputEvent(int gpio, long epochTime, long nanoTime, boolean value) {
		super(gpio, epochTime, nanoTime);

		this.value = value;
	}

	void setActiveHigh(boolean activeHigh) {
		this.activeHigh = activeHigh;
	}

	/**
	 * Returns the underlying GPIO state. Note does not compensate for active high /
	 * low.
	 * 
	 * @return the physical digital pin state
	 */
	public boolean getValue() {
		return value;
	}

	/**
	 * Determine if the event is active or not compensating for active high / low
	 * wiring
	 * 
	 * @return if the event should be consider active or inactive
	 */
	public boolean isActive() {
		return value == activeHigh;
	}

	@Override
	public String toString() {
		return "DigitalInputEvent [gpio=" + getGpio() + ", epochTime=" + getEpochTime() + ", nanoTime=" + getNanoTime()
				+ ", value=" + value + ", active=" + isActive() + "]";
	}
}
