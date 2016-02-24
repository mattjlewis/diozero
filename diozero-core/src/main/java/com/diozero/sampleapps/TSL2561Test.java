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

import com.diozero.TSL2561;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio10-0.3-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.TSL2561Test
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio11-0.3-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.TSL2561Test
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pi4j-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.TSL2561Test
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-wiringpi-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.TSL2561Test
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.TSL2561Test
 */
public class TSL2561Test {
	private static final int ITERATIONS = 20;

	public static void main(String[] args) {
		test();
	}
	
	public static void test() {
		try (TSL2561 tsl2561 = new TSL2561(TSL2561.TSL2561_PACKAGE_T_FN_CL)) {
			tsl2561.setAutoGain(true);

			for (int i=0; i<ITERATIONS; i++) {
				double lux = tsl2561.getLuminosity();
				Logger.info("Luminosity={} Lux", String.format("%.2f", Double.valueOf(lux)));

				SleepUtil.sleepSeconds(0.5);
			}
		} catch (RuntimeIOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		}
	}
}
