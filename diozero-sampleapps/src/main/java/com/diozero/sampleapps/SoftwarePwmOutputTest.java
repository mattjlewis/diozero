package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SoftwarePwmOutputTest.java
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

import com.diozero.devices.MCP23008;
import com.diozero.devices.PwmLed;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

public class SoftwarePwmOutputTest {
	public static void main(String[] args) {
		try (PwmOutputDeviceFactoryInterface device_factory = new MCP23008();
				PwmLed led0 = new PwmLed(device_factory, 0);
				PwmLed led1 = new PwmLed(device_factory, 1)) {
			led0.on();
			led1.on();
			SleepUtil.sleepSeconds(2);

			led0.off();
			led1.off();
			SleepUtil.sleepSeconds(2);

			led0.setValue(0.5f);
			led1.setValue(0.5f);
			SleepUtil.sleepSeconds(5);

			led0.pulse();
			led1.pulse();
			SleepUtil.sleepSeconds(10);
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			Diozero.shutdown();
		}
	}
}
