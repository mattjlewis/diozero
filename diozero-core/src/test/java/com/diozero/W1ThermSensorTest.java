package com.diozero;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     W1ThermSensorTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.devices.W1ThermSensor;
import com.diozero.devices.W1ThermSensor.Type;

@SuppressWarnings("static-method")
public class W1ThermSensorTest {
	private static Map<String, W1ThermSensor.Type> expectedTypes;
	private static Map<String, Double> expectedTemperatures;
	
	@BeforeClass
	public static void beforeClass() {
		expectedTypes = new HashMap<>();
		expectedTemperatures = new HashMap<>();
		
		expectedTypes.put("7894732432", W1ThermSensor.Type.DS18S20);
		expectedTemperatures.put("7894732432", Double.valueOf(21.187));
		
		expectedTypes.put("840932324", W1ThermSensor.Type.DS18S20);
		
		expectedTypes.put("4832984", W1ThermSensor.Type.DS1822);
		
		expectedTypes.put("02157190f0ff", W1ThermSensor.Type.DS18B20);
		expectedTemperatures.put("02157190f0ff", Double.valueOf(32.75));
		
		expectedTypes.put("5252532", W1ThermSensor.Type.MAX31850K);
	}
	
	@Test
	public void test() {
		System.out.println(Type.valueOf(Type.DS1822.getId()));
		System.out.println(Type.valueOf(Type.DS1822.name()));
		
		List<W1ThermSensor> sensors = W1ThermSensor.getAvailableSensors("src/test/resources/devices");
		Assert.assertEquals(5, sensors.size());
		for (W1ThermSensor sensor : sensors) {
			Logger.debug("Serial number=" + sensor.getSerialNumber());
			Logger.debug("Type=" + sensor.getType());
			Assert.assertEquals(expectedTypes.get(sensor.getSerialNumber()), sensor.getType());
			Double expected_temp = expectedTemperatures.get(sensor.getSerialNumber());
			if (expected_temp != null) {
				Logger.debug("Temperature={}", Float.valueOf(sensor.getTemperature()));
				Assert.assertEquals(expected_temp.doubleValue(), sensor.getTemperature(), 0.001);
			}
		}
	}
}
