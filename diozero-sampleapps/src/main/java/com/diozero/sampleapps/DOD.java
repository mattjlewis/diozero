package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     DOD.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

public class DOD {

	public static void main(String[] args) throws InterruptedException {

		float halfPeriod1 = 0.009433962f;
		float halfPeriod2 = 0.01923077f;

		try (DigitalOutputDevice step = new DigitalOutputDevice(21, true, false)) {

			System.out.println("Run");
			step.onOffLoop(halfPeriod1, halfPeriod1, DigitalOutputDevice.INFINITE_ITERATIONS, true, null);

			Thread.sleep(4000);

			step.off();

			Thread.sleep(250);

			System.out.println("Run again");

			step.onOffLoop(halfPeriod2, halfPeriod2, DigitalOutputDevice.INFINITE_ITERATIONS, true, null);

			Thread.sleep(4000);

			System.out.println("Stopping");

			step.off();

			System.out.println("All done");
		}
	}
}
