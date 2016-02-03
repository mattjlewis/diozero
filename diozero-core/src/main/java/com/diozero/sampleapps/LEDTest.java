package com.diozero.sampleapps;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.LED;
import com.diozero.util.SleepUtil;

/**
 * LED test application
 * To run:
 * (Pi4j):				sudo java -classpath dio-zero.jar:pi4j-core.jar com.diozero.sampleapps.LEDTest 17
 * (JDK Device I/O):	sudo java -classpath dio-zero.jar -Djava.security.policy=config/gpio.policy com.diozero.sampleapps.LEDTest 17
 */
public class LEDTest {
	private static final Logger logger = LogManager.getLogger(LEDTest.class);
	
	public static void main(String[] args) {
		if (args.length < 1) {
			logger.error("Usage: LEDTest <BCM pin number>");
			System.exit(1);
		}
		
		int pin = Integer.parseInt(args[0]);
		try (LED led = new LED(pin)) {
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
			
			logger.info("Blink 10 times");
			led.blink(0.5f, 0.5f, 10, false);
			
			logger.info("Done");
		} catch (IOException e) {
			logger.error("Error: " + e, e);
		}
	}
}
