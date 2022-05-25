package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     RoomScan.java
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

import com.diozero.api.I2CException;
import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.devices.GarminLidarLiteV4;
import com.diozero.devices.PiconZero;

public class RoomScan {
	// Degrees
	private static final float ANGLE_STEP = 2f;

	public static void main(String[] args) {
		ServoTrim trim = new ServoTrim(ServoTrim.DEFAULT_MID_US, ServoTrim.DEFAULT_90_DELTA_US,
				ServoTrim.DEFAULT_90_DELTA_US - 200);
		System.out.println(trim.getMinAngle() + ", " + trim.getMidAngle() + ", " + trim.getMaxAngle());

		try (PiconZero pz = new PiconZero();
				ServoDevice servo = ServoDevice.newBuilder(1).setDeviceFactory(pz).setTrim(trim).build();
				GarminLidarLiteV4 lidar = new GarminLidarLiteV4()) {
			lidar.configure(GarminLidarLiteV4.Preset.BALANCED);
			lidar.setPowerMode(GarminLidarLiteV4.PowerMode.ALWAYS_ON);
			lidar.setHighAccuracyMode(0x20);

			// Mid to max angle (degrees)
			for (float angle = trim.getMidAngle(); angle <= trim.getMaxAngle(); angle += ANGLE_STEP) {
				servo.setAngle(angle);
				int distance = lidar.getSingleReading();
				System.out.format("%.2f, %d%n", angle, distance);
				// SleepUtil.sleepMillis(5);
			}

			long start_ms = System.currentTimeMillis();
			// Max to min angle (degrees)
			for (float angle = trim.getMaxAngle(); angle >= trim.getMinAngle(); angle -= ANGLE_STEP) {
				servo.setAngle(angle);
				int distance = lidar.getSingleReading();
				System.out.format("%.2f, %d%n", angle, distance);
			}
			long sweep_duration_ms = System.currentTimeMillis() - start_ms;
			System.out.println("Sweep duration: " + sweep_duration_ms);

			for (int i = 0; i < 10; i++) {
				// Min to max angle (degrees)
				for (float angle = trim.getMinAngle(); angle <= trim.getMaxAngle(); angle += ANGLE_STEP) {
					servo.setAngle(angle);
					try {
						int distance = lidar.getSingleReading();
						System.out.format("%.2f, %d%n", angle, distance);
					} catch (I2CException e) {
						if (e.getError() != -121) {
							// Rethrow
							throw e;
						}
					}
					// SleepUtil.sleepMillis(5);
				}
				// Max to min angle (degrees)
				for (float angle = trim.getMaxAngle(); angle >= trim.getMinAngle(); angle -= ANGLE_STEP) {
					servo.setAngle(angle);
					try {
						int distance = lidar.getSingleReading();
						System.out.format("%.2f, %d%n", angle, distance);
					} catch (I2CException e) {
						if (e.getError() != -121) {
							// Rethrow
							throw e;
						}
					}
					// SleepUtil.sleepMillis(5);
				}
			}

			// Min to mid angle (degrees)
			for (float angle = trim.getMinAngle(); angle <= trim.getMidAngle(); angle += ANGLE_STEP) {
				servo.setAngle(angle);
				int distance = lidar.getSingleReading();
				System.out.format("%.2f, %d%n", angle, distance);
				// SleepUtil.sleepMillis(5);
			}
		}
	}
}
