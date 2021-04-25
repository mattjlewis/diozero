package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     BME280TestI2C.java
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

import com.diozero.devices.BME280;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

/**
 * Tests BME280 using I2C
 * 
 * @author gregflurry
 */
public class BME280TestI2C {
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		System.out.println("--- USING I2C ----");
		try (BME280 bme280 = new BME280()) {
			for (int i = 0; i < 3; i++) {
				bme280.waitDataAvailable(10, 5);
				float[] tph = bme280.getValues();
				float tF = tph[0] * (9f / 5f) + 32f;
				System.out.format("T=%.2f C (%.2f F) P=%.2f hPa H=%.2f%% RH%n", Float.valueOf(tph[0]), Float.valueOf(tF),
						Float.valueOf(tph[1]), Float.valueOf(tph[2]));

				SleepUtil.sleepSeconds(1);
			}
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.shutdown();
		}
	}
}
