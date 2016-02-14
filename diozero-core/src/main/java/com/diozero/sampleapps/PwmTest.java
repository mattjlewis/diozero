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

import com.diozero.api.PwmOutputDevice;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * PWM output test application, currently only works with Pi4j backend
 * PWM LED test application. Note doesn't work with the JDK Device I/O providers
 * Raspberry Pi BCM GPIO pins with hardware PWM support: 12 (phys 32, wPi 26), 13 (phys 33, wPi 23), 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
 * To run:
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PwmTest 18
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PwmTest 18
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar com.diozero.sampleapps.PwmTest 18
 */
public class PwmTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <pin number>", PwmTest.class.getName());
			System.exit(1);
		}
		
		test(Integer.parseInt(args[0]));
	}
	
	public static void test(int pin) {
		try (PwmOutputDevice pwm = new PwmOutputDevice(pin)) {
			for (float f=0; f<1; f+=0.05) {
				Logger.info("Setting value to {}", Float.valueOf(f));
				pwm.setValue(f);
				SleepUtil.sleepSeconds(0.5);
			}
			Logger.info("Done");
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: ", e);
		}
	}
}
