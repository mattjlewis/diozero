package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     HCSR04DioTest.java
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

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputOutputDevice;
import com.diozero.util.SleepUtil;

public class HCSR04DioTest {
	private static final double SPEED_OF_SOUND_CM_PER_S = 34029; // Approx Speed of Sound at sea level and 15 degC
	
	public static void main(String[] args) {
		try (DigitalInputOutputDevice dio = new DigitalInputOutputDevice(4, DeviceMode.DIGITAL_OUTPUT)) {
			for (int i=0; i<3; i++) {
				dio.setValue(true);
				SleepUtil.sleepSeconds(1);
				dio.setValue(false);
				SleepUtil.sleepSeconds(1);
			}
		}
		System.out.println("Starting");
		try (DigitalInputOutputDevice dio = new DigitalInputOutputDevice(20, DeviceMode.DIGITAL_OUTPUT)) {
			for (int i=0; i<3; i++) {
				dio.setMode(DeviceMode.DIGITAL_OUTPUT);
				System.out.println("setValue(true)");
				dio.setValue(true);
				SleepUtil.busySleep(10);
				System.out.println("setValue(false)");
				dio.setValue(false);
				dio.setMode(DeviceMode.DIGITAL_INPUT);
				SleepUtil.sleepSeconds(1);
			}

			for (int i=0; i<10; i++) {
				double distance = getDistance(dio);
				System.out.println("distance=" + distance);
				SleepUtil.sleepSeconds(1);
			}
		}
	}
	
	public static double getDistance(DigitalInputOutputDevice dio) {
		System.out.println("Starting test");
		dio.setMode(DeviceMode.DIGITAL_OUTPUT);
		System.out.println("setValue(true)");
		dio.setValue(true);
		SleepUtil.busySleep(10);
		dio.setValue(false);
		dio.setMode(DeviceMode.DIGITAL_INPUT);
		// Wait for the pin to go high
		long start = System.nanoTime();
		while (! dio.getValue()) {
			if (System.nanoTime() - start > 500_000_000) {
				Logger.error("Timeout exceeded waiting for echo to go high");
				return -1;
			}
		}
		long echo_on_time = System.nanoTime();
		// Wait for the pin to go low
		while (dio.getValue()) {
			if (System.nanoTime() - echo_on_time > 500_000_000) {
				Logger.error("Timeout exceeded waiting for echo to go low");
				return -1;
			}
		}
		long duration_ns = System.nanoTime() - echo_on_time;
		double ping_duration_s = (duration_ns) / (double) SleepUtil.NS_IN_SEC;
		
		// Distance = velocity * time taken
		// Half the ping duration as it is the time to the object and back
		return SPEED_OF_SOUND_CM_PER_S * (ping_duration_s / 2.0);
	}
}
