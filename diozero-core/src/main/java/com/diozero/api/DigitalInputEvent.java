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


public class DigitalInputEvent extends DeviceEvent {
	private boolean value;
	private boolean activeHigh;

	public DigitalInputEvent(int pin, long epochTime, long nanoTime, boolean value) {
		super(pin, epochTime, nanoTime);
		
		this.value = value;
	}
	
	void setAvtiveHigh(boolean activeHigh) {
		this.activeHigh = activeHigh;
	}

	/**
	 * Returns the underlying GPIO state. Note does not compensate for different pull up/down logic.
	 * @return underlying digital pin state
	 */
	public boolean getValue() {
		return value;
	}
	
	public boolean isActive() {
		return value & activeHigh;
	}

	@Override
	public String toString() {
		return "DigitalPinEvent [pin=" + getPin() + ", epochTime=" + getEpochTime() +
				", nanoTime=" + getNanoTime() + ", value=" + value + "]";
	}
}
