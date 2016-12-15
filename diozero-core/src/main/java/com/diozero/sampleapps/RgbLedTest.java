package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import org.pmw.tinylog.Logger;

import com.diozero.RgbLed;
import com.diozero.util.SleepUtil;

public class RgbLedTest {
	public static void main(String[] args) {
		int red_pin, green_pin, blue_pin;
		if (args.length > 2) {
			red_pin = Integer.parseInt(args[0]);
			green_pin = Integer.parseInt(args[1]);
			blue_pin = Integer.parseInt(args[2]);
		} else {
			red_pin = 17;
			green_pin = 27;
			blue_pin = 22;
		}
		test(red_pin, green_pin, blue_pin);
	}
	
	private static void test(int redPin, int greenPin, int bluePin) {
		try (RgbLed led = new RgbLed(redPin, greenPin, bluePin)) {
			Logger.info("Blue");
			led.setValues(false, false, true);	// 001
			SleepUtil.sleepSeconds(1);
			Logger.info("Green");
			led.setValues(false, true, false);	// 010
			SleepUtil.sleepSeconds(1);
			Logger.info("Blue + Green");
			led.setValues(false, true, true);	// 011
			SleepUtil.sleepSeconds(1);
			Logger.info("Red");
			led.setValues(true, false, false);	// 100
			SleepUtil.sleepSeconds(1);
			Logger.info("Red + Blue");
			led.setValues(true, false, true);	// 101
			SleepUtil.sleepSeconds(1);
			Logger.info("Red + Green");
			led.setValues(true, true, false);	// 110
			SleepUtil.sleepSeconds(1);
			Logger.info("Red + Green + Blue");
			led.setValues(true, true, true);		// 111
			SleepUtil.sleepSeconds(1);
		}
	}
}

