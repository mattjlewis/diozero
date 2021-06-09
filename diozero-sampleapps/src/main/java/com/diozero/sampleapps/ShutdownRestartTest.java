package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     ShutdownRestartTest.java
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

import org.tinylog.Logger;

import com.diozero.devices.Button;
import com.diozero.devices.LED;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

public class ShutdownRestartTest {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: " + ShutdownRestartTest.class.getName() + " <out-gpio> <in-gpio>");
			return;
		}
		int out_gpio = Integer.parseInt(args[0]);
		int in_gpio = Integer.parseInt(args[1]);

		for (int i = 0; i < 2; i++) {
			Logger.info("Loop #{}", Integer.valueOf(i));
			try (LED led = new LED(out_gpio); Button button = new Button(in_gpio)) {
				for (int x = 0; x < 2; x++) {
					led.on();
					SleepUtil.sleepMillis(500);
					led.off();
					SleepUtil.sleepMillis(500);
				}
			}

			SleepUtil.sleepMillis(500);
		}

		for (int i = 0; i < 2; i++) {
			Logger.info("Loop #{}", Integer.valueOf(i));
			try (LED led = new LED(out_gpio); Button button = new Button(in_gpio)) {
				for (int x = 0; x < 2; x++) {
					led.on();
					SleepUtil.sleepMillis(500);
					led.off();
					SleepUtil.sleepMillis(500);
				}
			} finally {
				Diozero.shutdown();
			}

			SleepUtil.sleepMillis(500);
		}
	}
}
