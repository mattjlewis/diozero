package com.diozero.sampleapps;

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


import org.pmw.tinylog.Logger;

import com.diozero.MCP23017;
import com.diozero.api.DigitalOutputDevice;

/**
 * MCP23017 performance test application. To run (note this hangs the Pi when using wiringPi provider):
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.MCP23017PerfTest 1}</li>
 * </ul>
 */
public class MCP23017PerfTest {
	private static final int ITERATIONS = 5_000;
	
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <mcp23017-output-pin> [<iterations>]", MCP23017PerfTest.class.getName());
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
		try (MCP23017 mcp23017 = new MCP23017();
				DigitalOutputDevice gpio = new DigitalOutputDevice(mcp23017, pin, true, false)) {
			for (int j=0; j<5; j++) {
				long start_nano = System.nanoTime();
				for (int i=0; i<iterations; i++) {
					gpio.setValueUnsafe(true);
					gpio.setValueUnsafe(false);
				}
				long duration_ns = System.nanoTime() - start_nano;
				
				Logger.info("Duration for {} iterations: {}s", Integer.valueOf(iterations),
						String.format("%.4f", Float.valueOf(((float)duration_ns) / 1000 / 1000 / 1000)));
			}
		}
	}
}
