package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     LedButtonTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import com.diozero.api.RuntimeInterruptedException;
import com.diozero.devices.LedButton;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

public class LedButtonTest {
	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <button-gpio> <led-gpio>");
			return;
		}

		int button_gpio = Integer.parseInt(args[0]);
		int led_gpio = Integer.parseInt(args[1]);

		int sleep_sec = 20;

		try (LedButton led_button = new LedButton(button_gpio, led_gpio)) {
			Logger.info("Sleeping for {} seconds", Integer.valueOf(sleep_sec));
			SleepUtil.sleepSeconds(sleep_sec);
		} catch (RuntimeInterruptedException e) {
			// Ignore
		} finally {
			Diozero.shutdown();
		}
	}
}
