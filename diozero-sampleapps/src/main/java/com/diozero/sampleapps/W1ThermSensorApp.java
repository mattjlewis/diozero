package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Sample applications
 * Filename:     W1ThermSensorApp.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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

/**
 * To run: 
 */
public class W1ThermSensorApp {
	public static void main(String[] args) {
		List<W1ThermSensor> sensors = W1ThermSensor.getAvailableSensors();
		if (sensors.isEmpty()) {
			Logger.warn("No W1-therm sensors detected");
			return;
		}
		Logger.info("Detected " + sensors.size() + " sensors");
		test(sensors.get(0));
	}
	
	private static void test(W1ThermSensor sensor) {
		for (int i=0; i<10; i++) {
			Logger.info("Serial num=" + sensor.getSerialNumber() + ", temp=" + sensor.getTemperature());
		}
	}
}
