package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     KeypadTest.java
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

import org.tinylog.Logger;

import com.diozero.devices.Keypad;
import com.diozero.util.SleepUtil;

public class KeypadTest {
	public static void main(String[] args) {
		/*-
		 * Wiring for my TinkerBoard:
		 *  8   7   6   5   4   3   2   1
		 * 162 163 171 223 187 188 185 224
		 */
		int[] row_gpios = { 162, 163, 171, 223 };
		int[] col_gpios = { 187, 188, 185, 224 };
		char[][] keys = { //
				{ '1', '2', '3', 'A' }, //
				{ '4', '5', '6', 'B' }, //
				{ '7', '8', '9', 'C' }, //
				{ '*', '0', '#', 'D' } //
		};
		try (Keypad keypad = new Keypad(row_gpios, col_gpios, keys)) {
			for (int i = 0; i < 10; i++) {
				Logger.debug("Pressed: {}", keypad.getKeys());
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
