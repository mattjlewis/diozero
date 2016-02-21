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

import com.diozero.PwmLed;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * PWM LED test application. Note doesn't work with the JDK Device I/O providers
 * Raspberry Pi BCM GPIO pins with hardware PWM support: 12 (phys 32, wPi 26), 13 (phys 33, wPi 23), 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
 * To run:
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pi4j-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PwmLedTest 18
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-wiringpi-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PwmLedTest 18
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.PwmLedTest 18
 */
public class PwmLedTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <BCM pin number>", PwmLedTest.class.getName());
			System.exit(1);
		}
		
		test(Integer.parseInt(args[0]));
	}
	
	public static void test(int pin) {
		float delay = 0.5f;
		
		try (PwmLed led = new PwmLed(pin)) {
			Logger.info("On");
			led.on();
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("Off");
			led.off();
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("Toggle");
			led.toggle();
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("Toggle");
			led.toggle();
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("25%");
			led.setValue(.25f);
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("Toggle (now 75%)");
			led.toggle();
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("50%");
			led.setValue(.5f);
			SleepUtil.sleepSeconds(delay);
			
			Logger.info("Blink 5 times");
			led.blink(0.5f, 0.5f, 5, false);
			
			Logger.info("Blink 5 times in the background");
			led.blink(0.5f, 0.5f, 5, true);
			for (int i=0; i<7; i++) {
				Logger.info("Sleeping for 1s");
				SleepUtil.sleepSeconds(1);
			}
			
			Logger.info("Fade in and out 5 times, on-off will take 1s; this will take 10s in total (2*1*5)");
			led.pulse(1, 50, 5, false);
			
			// FIXME Tests for background threads still running when shutting down
			Logger.info("Fade in and out 20 times in the background, on-off will take 0.5s; this will take 20s in total (2*0.5*20)");
			led.pulse(0.5f, 50, 20, true);
			for (int i=0; i<6; i++) {
				Logger.info("Sleeping for 1s");
				SleepUtil.sleepSeconds(1);
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
		
		Logger.info("Done");
	}
}
