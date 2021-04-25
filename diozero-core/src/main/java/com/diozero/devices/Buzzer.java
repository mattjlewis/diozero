package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Buzzer.java
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;

/**
 * Represents a digital buzzer component.
 */
public class Buzzer extends DigitalOutputDevice {

	/**
	 * @param gpio The GPIO to which the buzzer is attached to.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public Buzzer(int gpio) throws RuntimeIOException {
		super(gpio);
	}

	/**
	 * @param gpio The GPIO to which the buzzer is attached to.
	 * @param activeHigh Set to true if a high output value represents on.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public Buzzer(int gpio, boolean activeHigh) throws RuntimeIOException {
		super(gpio, activeHigh, false);
	}
	
	/**
	 * Beep repeatedly in a background thread.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void beep() throws RuntimeIOException {
		beep(1, 1, INFINITE_ITERATIONS, true);
	}
	
	/**
	 * Beep.
	 * 
	 * @param onTime
	 *            On time in seconds.
	 * @param offTime
	 *            Off time in seconds.
	 * @param n
	 *            Number of iterations. Set to &lt;0 to beep indefinitely
	 * @param background
	 *            If true start a background thread to control the blink and
	 *            return immediately. If false, only return once the blink
	 *            iterations have finished.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void beep(float onTime, float offTime, int n, boolean background) throws RuntimeIOException {
		onOffLoop(onTime, offTime, n, background, null);
	}
}
