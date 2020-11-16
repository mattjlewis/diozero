package com.diozero;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     HCSR04Test.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.devices.HCSR04;
import com.diozero.internal.provider.test.HCSR04EchoPin;
import com.diozero.internal.provider.test.HCSR04TriggerPin;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.util.SleepUtil;

@SuppressWarnings("static-method")
public class HCSR04Test {
	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setDigitalInputDeviceClass(HCSR04EchoPin.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(HCSR04TriggerPin.class);
	}
	
	@Test
	public void test() {
		try (HCSR04 hcsr04 = new HCSR04(26, 4)) {
			for (int i=0; i<6; i++) {
				float distance = hcsr04.getDistanceCm();
				Logger.info("Distance={}", Float.valueOf(distance));
				SleepUtil.sleepSeconds(0.5);
			}
		}
	}
}
