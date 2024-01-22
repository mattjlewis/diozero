package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     StringTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.tinylog.Logger;

import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.sbc.LocalSystemInfo;

public class StringTest {
	public static void main(String[] args) {
		String s = "ARMv6-compatible processor rev 7 (v6l)";
		// Split on '-' and ' '
		String[] parts = s.split("[- ]");
		System.out.println(parts.length);
		System.out.println(parts[0]);

		if (LocalSystemInfo.getInstance().isMacOS()) {
			// Assumes osx-cpu-temp has been installed (brew install osx-cpu-temp)
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(Runtime.getRuntime().exec("/usr/local/bin/osx-cpu-temp").getInputStream()))) {
				// Note that the output includes the non-ASCII degree symbol, e.g. 58.2<deg>C
				float temp = Float.parseFloat(br.readLine().replaceAll("[^0-9\\.]", ""));
				System.out.println(temp);
			} catch (IOException e) {
				// Ignore
				Logger.warn(e);
			}
		}

		// Normal formats
		System.out.println(RaspberryPiBoardInfoProvider.extractPwmGpioNumbers("dtoverlay=pwm,pin=12,func=4"));
		System.out.println(RaspberryPiBoardInfoProvider
				.extractPwmGpioNumbers("dtoverlay=pwm-2chan,pin=12,func=4,pin2=13,func2=4"));

		// Could be that pin and pin2 are in a different order
		System.out.println(RaspberryPiBoardInfoProvider
				.extractPwmGpioNumbers("dtoverlay=pwm-2chan,pin2=13,func2=4,pin=12,func=4"));
		// Could be that pin and func are in a different order
		System.out.println(RaspberryPiBoardInfoProvider.extractPwmGpioNumbers("dtoverlay=pwm,func=4,pin=12"));
		System.out.println(RaspberryPiBoardInfoProvider
				.extractPwmGpioNumbers("dtoverlay=pwm-2chan,func2=4,pin2=13,pin=12,func=4"));
	}
}
