package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     InputWaitTest.java  
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

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.DigitalInputDevice;

/**
 * Input test application. To run:
 * <ul>
 * <li>Built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.InputWaitTest 25}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.InputWaitTest 25}</li>
 * </ul>
 */
public class InputWaitTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <input-pin>", InputWaitTest.class.getName());
			System.exit(1);
		}
		test(Integer.parseInt(args[0]));
	}

	public static void test(int inputPin) {
		try (DigitalInputDevice input = new DigitalInputDevice(inputPin, GpioPullUpDown.PULL_UP,
				GpioEventTrigger.BOTH)) {
			while (true) {
				Logger.info("Waiting for 2000ms for button press");
				boolean notified = input.waitForValue(false, 2000);
				Logger.info("Timed out? " + !notified);
				Logger.info("Waiting for 2000ms for button release");
				notified = input.waitForValue(true, 2000);
				Logger.info("Timed out? " + !notified);
			}
		} catch (RuntimeIOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		} catch (InterruptedException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
