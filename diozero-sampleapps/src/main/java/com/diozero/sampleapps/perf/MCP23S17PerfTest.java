package com.diozero.sampleapps.perf;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     MCP23S17PerfTest.java
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.SpiConstants;
import com.diozero.devices.MCP23S17;
import com.diozero.devices.mcp23xxx.MCP23x17;
import com.diozero.devices.mcp23xxx.MCP23xxx;

/**
 * MCP23017 performance test application. To run (note this hangs the Pi when
 * using wiringPi provider):
 * <ul>
 * <li>built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * </ul>
 */
public class MCP23S17PerfTest {
	private static final int ITERATIONS = 5_000;

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <mcp23s17-output-pin> [<iterations>]", MCP23S17PerfTest.class.getName());
			System.exit(1);
		}
		int output_pin = Integer.parseInt(args[0]);

		int iterations = ITERATIONS;
		if (args.length > 1) {
			iterations = Integer.parseInt(args[1]);
		}

		test(output_pin, iterations);
	}

	private static void test(int pin, int iterations) {
		int board_address = 0;
		try (MCP23x17 mcp23x17 = new MCP23S17(SpiConstants.DEFAULT_SPI_CONTROLLER, SpiConstants.CE1, board_address,
				MCP23S17.MAX_CLOCK_SPEED, MCP23xxx.INTERRUPT_GPIO_NOT_SET, MCP23xxx.INTERRUPT_GPIO_NOT_SET);
				DigitalOutputDevice gpio = new DigitalOutputDevice(mcp23x17, pin, true, false)) {
			for (int j = 0; j < 5; j++) {
				long start_nano = System.nanoTime();
				for (int i = 0; i < iterations; i++) {
					gpio.setValue(true);
					gpio.setValue(false);
				}
				long duration_ns = System.nanoTime() - start_nano;

				Logger.info("Duration for {} iterations: {}s", Integer.valueOf(iterations),
						String.format("%.4f", Float.valueOf(((float) duration_ns) / 1000 / 1000 / 1000)));
			}

			Logger.info("Going direct to the MCP23x17");
			for (int j = 0; j < 5; j++) {
				long start_nano = System.nanoTime();
				for (int i = 0; i < iterations; i++) {
					mcp23x17.setValue(pin, true);
					mcp23x17.setValue(pin, false);
				}
				long duration_ns = System.nanoTime() - start_nano;

				Logger.info("Duration for {} iterations: {}s", Integer.valueOf(iterations),
						String.format("%.4f", Float.valueOf(((float) duration_ns) / 1000 / 1000 / 1000)));
			}
		}
	}
}
