package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     DebouncedDigitalInputTest.java  
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

import org.tinylog.Logger;

import com.diozero.api.GpioPullUpDown;
import com.diozero.api.sandpit.DebouncedDigitalInputDevice;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

public class DebouncedDigitalInputTest {
	public static void main(String[] args) {
		int gpio = 5;
		if (args.length > 0) {
			gpio = Integer.parseInt(args[0]);
		}

		int debounce_time_ms = 10;
		if (args.length > 1) {
			debounce_time_ms = Integer.parseInt(args[1]);
		}

		int delay_sec = 10;

		try (DebouncedDigitalInputDevice d = new DebouncedDigitalInputDevice(gpio, GpioPullUpDown.PULL_UP,
				debounce_time_ms)) {
			d.whenActivated(timestamp -> System.out.println("hit " + timestamp));
			d.whenDeactivated(timestamp -> System.out.println("away " + timestamp));
			d.addListener(event -> System.out.println(event));

			System.out.println("Sleeping for " + delay_sec + " seconds...");
			SleepUtil.sleepSeconds(delay_sec);
			System.out.println("Done.");

			SleepUtil.sleepSeconds(1);
			System.out.println("Waiting for button to be pressed...");
			d.waitForActive();
		} catch (InterruptedException e) {
			Logger.error(e, "Interrupted: {}", e);
		} finally {
			DeviceFactoryHelper.shutdown();
		}
	}
}
