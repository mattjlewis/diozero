package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.MCP23017;
import com.diozero.api.DigitalOutputDevice;

/**
 * To run (note this hangs the Pi when using wiringPi provider):
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio10-0.3-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017PerfTest 1
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio11-0.3-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017PerfTest 1
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pi4j-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017PerfTest 1
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-wiringpi-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017PerfTest 1
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.MCP23017PerfTest 1
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
