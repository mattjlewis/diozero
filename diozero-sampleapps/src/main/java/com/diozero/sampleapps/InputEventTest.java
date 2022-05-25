package com.diozero.sampleapps;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     InputEventTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.Diozero;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

public class InputEventTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <input-pin> [pud]", InputEventTest.class.getName());
			System.exit(1);
		}
		GpioPullUpDown pud = GpioPullUpDown.PULL_UP;
		if (args.length > 1) {
			pud = GpioPullUpDown.valueOf(args[1]);
		}
		test(21, Integer.parseInt(args[0]), pud);
	}

	public static void test(final int outputPin, final int inputPin, final GpioPullUpDown pud) {
		final int delay_s = 5;
		try (final DigitalOutputDevice dod = new DigitalOutputDevice(outputPin)) {
			Future<?> future = DiozeroScheduler.getDefaultInstance().submit(() -> {
				long start_ms = System.currentTimeMillis();
				do {
					dod.on();
					dod.off();
				} while ((System.currentTimeMillis() - start_ms) < (delay_s + 3) * 1_000);
			});

			try (final DigitalInputDevice did = new DigitalInputDevice.Builder(inputPin).setPullUpDown(pud).build()) {
				did.addListener(event -> Logger.trace("Event: {}", event));

				Logger.info("Waiting for {}s for input events on GPIO {}", Integer.valueOf(delay_s),
						Integer.valueOf(inputPin));
				SleepUtil.sleepSeconds(delay_s);
				Logger.info("Closing did...");
			}

			Logger.info("Waiting for future to complete.");
			future.get();
			Logger.info("Done.");
		} catch (RuntimeIOException | ExecutionException | InterruptedException e) {
			Logger.error(e, "Error: {}", e);
		} finally {
			Diozero.shutdown();
		}
	}
}
