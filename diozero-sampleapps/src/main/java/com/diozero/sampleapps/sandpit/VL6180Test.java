package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     VL6180Test.java
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

import com.diozero.devices.sandpit.VL6180;
import com.diozero.util.SleepUtil;

public class VL6180Test {
	public static void main(String[] args) {
		try (VL6180 vl6180 = new VL6180()) {
			System.out.format("Model id: 0x%x%n", vl6180.getModelId());
			System.out.format("Model revision: v%d.%d%n", vl6180.getModelMajor(), vl6180.getModelMinor());
			System.out.format("Module revision: v%d.%d%n", vl6180.getModuleMajor(), vl6180.getModuleMinor());
			System.out.format("Manufactured on: %s, phase %d%n", vl6180.getManufactureDateTime(),
					vl6180.getManufacturePhase());

			for (int i = 0; i < 10; i++) {
				System.out.println("Distance: " + vl6180.getDistanceCm() + " cm");
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
