package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     MotionTest.java
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

import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.sandpit.MotionSensor;
import com.diozero.util.SleepUtil;

public class MotionTest implements AutoCloseable {
	public static void main(String[] args) {
		try (MotionTest test = new MotionTest(19, 26)) {
			Logger.info("Sleeping for 60s");
			SleepUtil.sleepSeconds(60);
		}
	}
	
	private List<DigitalInputDevice> sensors;
	
	private MotionTest(int... pins) {
		sensors = new ArrayList<>();
		
		for (int pin : pins) {
			//DigitalInputDevice sensor = new MotionSensor(pin);
			DigitalInputDevice sensor;
			if (pin == 26) {
				// Fudge for this strange type of PIR:
				// http://skpang.co.uk/catalog/pir-motion-sensor-p-796.html
				// Red (5V) / White (Ground) / Black (open collector Alarm)
				// The alarm pin is an open collector meaning you will need a pull up resistor on the alarm pin
				// Signal motion if there are 5 or more alarms in a 200ms period, check every 50ms
				sensor = new MotionSensor(pin, GpioPullUpDown.PULL_UP, 5, 200, 50);
			} else {
				sensor = new DigitalInputDevice(pin, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.BOTH);
			}
			Logger.info("Created sensor on pin " + pin + " pud=" + sensor.getPullUpDown() + ", trigger=" + sensor.getTrigger());
			sensor.whenActivated(nanoTime ->System.out.println("Pin " + pin + " activated"));
			sensor.whenDeactivated(nanoTime ->System.out.println("Pin " + pin + " deactivated"));
			sensors.add(sensor);
		}
	}

	@Override
	public void close() {
		for (DigitalInputDevice sensor : sensors) {
			sensor.close();
		}
	}
}
