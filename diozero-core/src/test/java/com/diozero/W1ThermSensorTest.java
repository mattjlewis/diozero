package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.devices.W1ThermSensor;
import com.diozero.devices.W1ThermSensor.Type;

public class W1ThermSensorTest {
	
	public static void main(String[] args) {
		System.out.println(Type.valueOf(Type.DS1822.getId()));
		System.out.println(Type.valueOf(Type.DS1822.name()));
		
		List<W1ThermSensor> sensors = W1ThermSensor.getAvailableSensors("src/test/resources/devices");
		System.out.println(sensors);
		for (W1ThermSensor sensor : sensors) {
			Logger.debug("Type=" + sensor.getType());
			Logger.debug("Serial number=" + sensor.getSerialNumber());
			if (sensor.getSerialNumber().startsWith("789") || sensor.getSerialNumber().startsWith("021")) {
				Logger.debug("Temperature={}", Float.valueOf(sensor.getTemperature()));
			}
		}
	}
}
