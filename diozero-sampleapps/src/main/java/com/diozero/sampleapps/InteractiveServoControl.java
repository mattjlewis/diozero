package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     InteractiveServoControl.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.tinylog.Logger;

import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.devices.PCA9685;
import com.diozero.internal.spi.ServoDeviceFactoryInterface;

public class InteractiveServoControl {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <i2c-controller> [device-address-as-hex,default=40]",
					InteractiveServoControl.class.getName());
			System.exit(1);
		}

		int i2c_controller = Integer.parseInt(args[0]);
		int device_address = PCA9685.DEFAULT_ADDRESS;
		if (args.length > 1) {
			device_address = Integer.parseInt(args[1], 16);
		}
		ServoTrim trim = ServoTrim.MG996R;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				PCA9685 pca9685 = new PCA9685(i2c_controller, device_address, 50)) {
			while (true) {
				System.out.print("GPIO ('q' to quit): ");
				System.out.flush();
				String line = br.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.equals("") || line.equals("q")) {
					break;
				}
				try {
					int gpio = Integer.parseInt(line);
					run(br, pca9685, gpio, trim);
				} catch (NumberFormatException e) {
					System.out.println("Invalid integer '" + line + "'");
				}
			}
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	private static void run(BufferedReader br, ServoDeviceFactoryInterface deviceFactory, int gpio, ServoTrim trim)
			throws IOException {
		try (ServoDevice servo = ServoDevice.Builder.builder(gpio).setDeviceFactory(deviceFactory).setTrim(trim)
				.build()) {
			while (true) {
				System.out.print("[" + gpio + "] Angle ('q' to quit): ");
				System.out.flush();
				String line = br.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.equals("") || line.equals("q")) {
					break;
				}
				try {
					float angle = Float.parseFloat(line);
					servo.setAngle((int) angle);
					int pulse_width_us = servo.getPulseWidthUs();
					float new_angle = servo.getAngle();
					Logger.debug("[" + gpio + "] pulse_width_ms: {}, new_angle: {}", Integer.valueOf(pulse_width_us),
							Float.valueOf(new_angle));
				} catch (NumberFormatException e) {
					System.out.println("Invalid float '" + line + "'");
				}
			}
		}
	}
}
