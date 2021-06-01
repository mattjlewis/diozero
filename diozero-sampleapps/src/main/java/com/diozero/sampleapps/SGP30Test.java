package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SGP30Test.java
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

import com.diozero.devices.SGP30;
import com.diozero.devices.SGP30.FeatureSetVersion;
import com.diozero.devices.SGP30.SGP30Measurement;
import com.diozero.util.SleepUtil;

public class SGP30Test {
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}
		int readings = 15;
		if (args.length > 1) {
			readings = Integer.parseInt(args[1]);
		}

		try (SGP30 sgp30 = new SGP30(controller)) {
			// Product Type should be SGP30_PRODUCT_TYPE
			// Product Version should be 0x22
			FeatureSetVersion fsv = sgp30.getFeatureSetVersion();
			System.out.println(fsv);
			if (fsv.getProductType() != SGP30.PRODUCT_TYPE) {
				Logger.error("Incorrect product type - got {}, expected {}", Integer.valueOf(fsv.getProductType()),
						Integer.valueOf(SGP30.PRODUCT_TYPE));
			}

			System.out.format("Serial Id: 0x%X%n", Long.valueOf(sgp30.getSerialId()));

			System.out.println("Baseline: " + sgp30.getIaqBaseline());
			System.out.println("Total VOC Inceptive Baseline: " + sgp30.getTvocInceptiveBaseline());
			System.out.println("Measure test: " + sgp30.measureTest());

			System.out.println("Raw: " + sgp30.rawMeasurement());

			// Note that the first 15 readings should be discarded (co2Equiv=400, tVOC=0)
			sgp30.start(SGP30Test::printMeasurement);
			SleepUtil.sleepSeconds(readings + SGP30.IGNORE_READINGS);
			sgp30.stop();

			System.out.println("Raw: " + sgp30.rawMeasurement());

			// Repeat to verify that the initial ~15 reading are 400:0
			sgp30.start(SGP30Test::printMeasurement);
			SleepUtil.sleepSeconds(readings + SGP30.IGNORE_READINGS);
			sgp30.stop();

			System.out.println("Raw: " + sgp30.rawMeasurement());
		}
	}

	private static void printMeasurement(SGP30Measurement measurement) {
		int reading = measurement.getReading();
		if (reading <= SGP30.IGNORE_READINGS) {
			System.out.format("Warming up - ignore, measurement[%d]: %s%n", Integer.valueOf(reading), measurement);
			if (measurement.getCO2Equivalent() != 400) {
				System.out.println(
						"Expected warming up CO2 Equiv reading to be 400, got " + measurement.getCO2Equivalent());
			}
			if (measurement.getTotalVOC() != 0) {
				System.out.println("Expected warming up tVOC reading to be 0, got " + measurement.getTotalVOC());
			}
		} else {
			System.out.format("Measurement [%d]: CO2 Equivalent: %,d, Total VOC: %,d %n", Integer.valueOf(reading),
					Integer.valueOf(measurement.getCO2Equivalent()), Integer.valueOf(measurement.getTotalVOC()));
		}
	}
}
