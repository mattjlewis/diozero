package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     PwmOutputTest.java
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

import com.diozero.api.PwmOutputDevice;
import com.diozero.util.SleepUtil;

public class PwmOutputTest {
	public static void main(String[] args) {
		try (PwmOutputDevice pwm = new PwmOutputDevice(21)) {
			for (int i = 0; i < 5; i++) {
				System.out.println("50Hz");
				pwm.setPwmFrequency(50);
				for (float val = 0.1f; val <= 1; val += 0.1f) {
					System.out.println("Val: " + val);
					pwm.setValue(val);
					SleepUtil.sleepSeconds(1);
				}

				System.out.println("100Hz");
				pwm.setPwmFrequency(100);
				for (float val = 0.1f; val <= 1; val += 0.1f) {
					System.out.println("Val: " + val);
					pwm.setValue(val);
					SleepUtil.sleepSeconds(1);
				}

				float val = 0.5f;
				pwm.setValue(val);
				for (int x = 1; x < 100; x++) {
					System.out.println("PWM Freq: " + x + ", Val: " + val);
					pwm.setPwmFrequency(x);
					SleepUtil.sleepMillis(100);
				}
			}
		}
	}
}
