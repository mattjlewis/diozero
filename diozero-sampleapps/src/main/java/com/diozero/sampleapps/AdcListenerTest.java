package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     AdcListenerTest.java  
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

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Sample application to illustrate listening for changes to analog values. To run:
 * <ul>
 * <li>Built-in:<br>
 *  {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.AdcListenerTest 0}</li>
 * <li>pigpgioj:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.AdcListenerTest 0}</li>
 * </ul>
 */
public class AdcListenerTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <adc-number>", AdcListenerTest.class.getName());
			System.exit(2);
		}

		int adc_number = Integer.parseInt(args[0]);
		float vref = 1.8f;
		test(adc_number, vref);
	}

	public static void test(int adcNumber, float vRef) {
		try (AnalogInputDevice adc = new AnalogInputDevice(adcNumber, vRef)) {
			//adc.addListener((event) -> Logger.info("Event: {}", event));
			for (int i = 0; i < 10; i++) {
				Logger.info("Scaled: {}, Unscaled: {}", Float.valueOf(adc.getScaledValue()),
						Float.valueOf(adc.getUnscaledValue()));
				SleepUtil.sleepSeconds(1);
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
