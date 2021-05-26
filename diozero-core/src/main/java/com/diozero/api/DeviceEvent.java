package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DeviceEvent.java
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

/**
 * Abstract class representing an event from a GPIO device.
 */
public abstract class DeviceEvent extends Event {
	private int gpio;

	/**
	 * Constructor for a device event.
	 * 
	 * @param gpio      GPIO number
	 * @param epochTime Unix epoch time: milliseconds elapsed since January 1, 1970
	 *                  (midnight UTC/GMT), not counting leap seconds
	 * @param nanoTime  The Java Virtual Machine's high-resolution time source, in
	 *                  nanoseconds (note this is unrelated to epochTime and uses
	 *                  CLOCK_MONOTONIC in the C clock_gettime() function)
	 */
	public DeviceEvent(int gpio, long epochTime, long nanoTime) {
		super(epochTime, nanoTime);
		this.gpio = gpio;
	}

	public int getGpio() {
		return gpio;
	}
}
