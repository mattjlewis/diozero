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


import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.PwmLed;
import com.diozero.util.SleepUtil;

/**
 * PWM LED test application, currently only works with Pi4j backend
 * To run:
 * (Pi4j):				sudo java -classpath dio-zero.jar:pi4j-core.jar com.diozero.sampleapps.PwmLedTest 12
 * (JDK Device I/O):	sudo java -classpath dio-zero.jar -Djava.security.policy=config/gpio.policy com.diozero.sampleapps.PwmLedTest 12
 * Raspberry Pi BCM GPIO pins with hardware PWM support: 12 (phys 32, wPi 26), 13 (phys 33, wPi 23), 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
 */
public class PwmLedTest {
	private static final Logger logger = LogManager.getLogger(PwmLedTest.class);
	
	public static void main(String[] args) {
		if (args.length < 1) {
			logger.error("Usage: LEDTest <BCM pin number>");
			System.exit(1);
		}
		
		int pin = Integer.parseInt(args[0]);
		try (PwmLed led = new PwmLed(pin, 100)) {
			logger.info("On");
			led.on();
			SleepUtil.sleepSeconds(1);
			logger.info("Off");
			led.off();
			SleepUtil.sleepSeconds(1);
			logger.info("Toggle");
			led.toggle();
			SleepUtil.sleepSeconds(1);
			logger.info("Toggle");
			led.toggle();
			SleepUtil.sleepSeconds(1);
			logger.info("25%");
			led.setValue(.25f);
			SleepUtil.sleepSeconds(1);
			logger.info("Toggle (now 75%)");
			led.toggle();
			SleepUtil.sleepSeconds(1);
			logger.info("50%");
			led.setValue(.5f);
			SleepUtil.sleepSeconds(1);
			
			logger.info("Blink 10 times");
			led.blink(0.5f, 0.5f, 10, false);
			
			logger.info("Fade in and out 10 times");
			led.pulse(1, 50, 10, false);
			
			logger.info("Done");
		} catch (IOException e) {
			logger.error("Error: " + e, e);
			e.printStackTrace();
		}
	}
}
