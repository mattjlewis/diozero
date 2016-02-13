package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

import com.diozero.LDR;
import com.diozero.MCP3008;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * 
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LDRTest 0 3
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LDRTest 0 3
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.LDRTest 0 3
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.LDRTest 0 3
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar -Djava.library.path=. com.diozero.sampleapps.LDRTest 0 3
 */
public class LDRTest {
	private static final int ITERATIONS = 20;

	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: LDRTest <chip-select> <adc-pin>");
			System.exit(2);
		}
		int chip_select = Integer.parseInt(args[0]);
		int adc_pin = Integer.parseInt(args[1]);
		float vref = 3.3f;
		int r1 = 10_000;
		test(chip_select, adc_pin, vref, r1);
	}
	
	public static void test(int chipSelect, int pin, float vRef, int r1) {
		try (MCP3008 mcp3008 = new MCP3008(chipSelect); LDR ldr = new LDR(mcp3008, pin, vRef, r1)) {
			ldr.addListener(ldr, r1);
			for (int i=0; i<ITERATIONS; i++) {
				double lux = ldr.getLuminosity();
				Logger.info("Lux: {}", String.format("%.2f", Double.valueOf(lux)));
				SleepUtil.sleepSeconds(.5);
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
