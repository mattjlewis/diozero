package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     InputTimingTest.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioPullUpDown;

public class InputTimingTest {
	private static final String CONSUMER = "consumer";
	private static final String PRODUCER = "producer";

	public static void main(String[] args) throws InterruptedException {
		if (args.length < 2) {
			System.out.println("Usage: " + InputTimingTest.class.getName()
					+ " <producer|consumer> <output-gpio> [<input-gpio>] [init-method]");
		}

		int arg_index = 0;
		String mode = args[arg_index++].trim();

		int output_gpio = Integer.parseInt(args[arg_index++]);
		if (mode.equals(CONSUMER)) {
			int input_gpio = Integer.parseInt(args[arg_index++]);
			consumer(output_gpio, input_gpio);
		} else if (mode.equals(PRODUCER)) {
			String init_method = "";
			if (arg_index < args.length) {
				init_method = args[arg_index++];
			}
			producer(output_gpio, init_method);
		} else {
			System.out.println("Error: Invalid mode '" + mode + "'");
		}
	}

	public static void consumer(int outputGpio, int inputGpio) throws InterruptedException {
		try (DigitalOutputDevice dod = new DigitalOutputDevice(outputGpio, true, false);
				DigitalInputDevice did = DigitalInputDevice.Builder.builder(inputGpio)
						.setPullUpDown(GpioPullUpDown.NONE).setActiveHigh(false).build();) {

			System.out.println("Ready to wait ...");

			int num = 3;
			for (int i = 0; i < num; i++) {
				boolean status = did.waitForActive(10_000);
				dod.on();
				if (status) {
					System.out.println("Got it");
				}
				dod.off();
			}

			System.out.println("Done");
		}
	}

	public static void producer(int outputGpio, String initMethod) throws InterruptedException {
		try (DigitalOutputDevice dod = new DigitalOutputDevice(outputGpio, false, false)) {
			switch (initMethod) {
			case "1":
				System.out.println("DOD is on? " + dod.isOn());
				break;
			case "2":
				System.out.print("[scanner] Enter when ready to pulse > ");
				Scanner s1 = new Scanner(System.in);
				s1.nextLine();
				break;
			case "3":
				System.out.print("[twr scanner] Enter when ready to pulse > ");
				try (Scanner s2 = new Scanner(System.in)) {
					s2.nextLine();
				}
				break;
			case "4":
				System.out.print("[twr br] Enter when ready to pulse > ");
				try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
					br.readLine();
				} catch (IOException e) {
					// Ignore
					System.out.println("Error: " + e);
					e.printStackTrace();
				}
				break;
			default:
			}

			int num = 500;
			int delay_ms = 5;

			// Pulse start time (ns)
			long[] tpb = new long[num];
			// Pulse end time (ns)
			long[] tpe = new long[num];

			for (int i = 0; i < num; i++) {
				tpb[i] = System.nanoTime();
				dod.on();
				dod.off();
				tpe[i] = System.nanoTime();

				Thread.sleep(delay_ms);
			}

			float avg = 0;
			for (int i = 0; i < tpe.length; i++) {
				long pw_us = (tpe[i] - tpb[i]) / 1_000;
				if (i < 20) {
					System.out.println("Pulse " + (1 + i) + " width (microseconds) = " + pw_us);
				}
				avg += (pw_us - avg) / (i + 1);
			}
			System.out.format("Average pulse width: %.2f microseconds%n", Float.valueOf(avg));

			avg = 0;
			for (int i = 0; i < tpb.length - 1; i++) {
				long between_ms = (tpb[i + 1] - tpb[i]) / 1_000_000;
				if (i < 20) {
					System.out.println("Between pulses " + i + " and " + (i + 1) + " (milliseconds) = " + between_ms);
				}
				avg += (between_ms - avg) / (i + 1);
			}
			System.out.format("Average between pulses: %.2f milliseconds%n", Float.valueOf(avg));
		}
	}
}
