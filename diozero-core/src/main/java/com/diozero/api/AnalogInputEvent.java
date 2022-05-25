package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AnalogInputEvent.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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
 * Represents an input event from an analog device.
 */
public class AnalogInputEvent extends DeviceEvent {
	private float unscaledValue;
	private float range;

	/**
	 * Constructs an input event from an analog device. See (@link
	 * com.diozero.api.DeviceEvent} for first 3 parameters.
	 *
	 * @param gpio          the gpio number that this event is related to
	 * @param epochTime     time in milliseconds since Jan 1st 1970
	 * @param nanoTime      monotonic time in nanoseconds
	 * @param unscaledValue initial value (unscaled)
	 */
	public AnalogInputEvent(int gpio, long epochTime, long nanoTime, float unscaledValue) {
		super(gpio, epochTime, nanoTime);

		this.unscaledValue = unscaledValue;
		this.range = 1;
	}

	/**
	 * Get the maximum scaled value for the analog input device that generated this
	 * event
	 *
	 * @return the maximum scaled value
	 */
	public float getRange() {
		return range;
	}

	void setRange(float range) {
		this.range = range;
	}

	/**
	 * Value from -1..1
	 *
	 * @return the unscaled value
	 */
	public float getUnscaledValue() {
		return unscaledValue;
	}

	/**
	 * Value from -range..range
	 *
	 * @return the scaled value
	 */
	public float getScaledValue() {
		return unscaledValue * range;
	}

	@Override
	public String toString() {
		return "AnalogPinEvent [pin=" + getGpio() + ", epochTime=" + getEpochTime() + ", nanoTime=" + getNanoTime()
				+ ", unscaledValue=" + unscaledValue + ", range=" + range + "]";
	}
}
