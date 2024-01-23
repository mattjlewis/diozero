package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     PudTest.java
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

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.SleepUtil;

public class PudTest {
	public static void main(String[] args) {
		System.out.println("PUD None");
		try (DigitalInputDevice did = DigitalInputDevice.Builder.builder(22).setPullUpDown(GpioPullUpDown.NONE)
				.build()) {
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
		}

		SleepUtil.sleepSeconds(0.5);
		System.out.println("PUD Pull Up");
		try (DigitalInputDevice did = DigitalInputDevice.Builder.builder(22).setPullUpDown(GpioPullUpDown.PULL_UP)
				.build()) {
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
		}

		SleepUtil.sleepSeconds(0.5);
		System.out.println("PUD Pull Down");
		try (DigitalInputDevice did = DigitalInputDevice.Builder.builder(22).setPullUpDown(GpioPullUpDown.PULL_DOWN)
				.build()) {
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
		}
	}
}
