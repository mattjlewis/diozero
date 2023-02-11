package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     BMx280TestI2C.java
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

import com.diozero.api.I2CConstants;
import com.diozero.devices.BMx280;
import com.diozero.devices.ThermometerInterface;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

/**
 * Tests BMP280/BME280 using I2C
 *
 * @author gregflurry
 */
public class BMx280TestI2C {
	public static void main(String[] args) {
		int i2c_bus = I2CConstants.CONTROLLER_1;
		if (args.length > 0) {
			i2c_bus = Integer.parseInt(args[0]);
		}
		int iterations = 10;
		if (args.length > 1) {
			iterations = Integer.parseInt(args[1]);
		}
		System.out.println("--- USING I2C-" + i2c_bus + " ---");
		try (BMx280 bmx280 = BMx280.I2CBuilder.builder(i2c_bus).build()) {
			for (int i = 0; i < iterations; i++) {
				bmx280.waitDataAvailable(10, 5);
				float[] tph = bmx280.getValues();
				System.out.format("Temperature: %.2f C (%.2f F), Pressure: %.2f hPa", Float.valueOf(tph[0]),
						Float.valueOf(ThermometerInterface.celsiusToFahrenheit(tph[0])), Float.valueOf(tph[1]));
				if (bmx280.getModel() == BMx280.Model.BME280) {
					System.out.format(", Relaative Humidity: %.2f%% RH", Float.valueOf(tph[2]));
				}
				System.out.println();

				SleepUtil.sleepSeconds(1);
			}
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			Diozero.shutdown();
		}
	}
}
