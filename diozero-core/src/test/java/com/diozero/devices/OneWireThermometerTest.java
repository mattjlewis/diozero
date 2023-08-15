package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OneWireThermometerTest.java
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

@SuppressWarnings("static-method")
public class OneWireThermometerTest {
	private static Map<String, OneWireDeviceType> expectedTypes;
	private static Map<String, Double> expectedTemperatures;

	@BeforeAll
	public static void beforeAll() {
		expectedTypes = new HashMap<>();
		expectedTemperatures = new HashMap<>();

		expectedTypes.put("7894732432", OneWireDeviceType.DS18S20);
		expectedTemperatures.put("7894732432", Double.valueOf(21.187));

		expectedTypes.put("840932324", OneWireDeviceType.DS18S20);

		expectedTypes.put("4832984", OneWireDeviceType.DS1822);

		expectedTypes.put("02157190f0ff", OneWireDeviceType.DS18B20);
		expectedTemperatures.put("02157190f0ff", Double.valueOf(32.75));

		expectedTypes.put("5252532", OneWireDeviceType.MAX31850K);
	}

	@Test
	public void test() {
		System.out.println(OneWireDeviceType.valueOf(OneWireDeviceType.DS1822.getId()));
		System.out.println(OneWireDeviceType.valueOf(OneWireDeviceType.DS1822.name()));

		List<OneWireThermometer> sensors = OneWireThermometer.getAvailableSensors("src/test/resources/devices");
		Assertions.assertEquals(5, sensors.size());
		for (OneWireThermometer sensor : sensors) {
			Logger.debug("Serial number=" + sensor.getSerialNumber());
			Logger.debug("Type=" + sensor.getType());
			Assertions.assertEquals(expectedTypes.get(sensor.getSerialNumber()), sensor.getType());
			Double expected_temp = expectedTemperatures.get(sensor.getSerialNumber());
			if (expected_temp != null) {
				Logger.debug("Temperature={}", Float.valueOf(sensor.getTemperature()));
				Assertions.assertEquals(expected_temp.doubleValue(), sensor.getTemperature(), 0.001);
			}
		}
	}
}
