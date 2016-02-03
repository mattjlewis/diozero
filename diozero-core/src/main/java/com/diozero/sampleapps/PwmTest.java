package com.diozero.sampleapps;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.PwmOutputDevice;
import com.diozero.util.SleepUtil;

/**
 * PWM output test application, currently only works with Pi4j backend
 * To run:
 * (Pi4j):				sudo java -classpath dio-zero.jar:pi4j-core.jar com.diozero.sampleapps.PwmTest 12
 * (JDK Device I/O):	sudo java -classpath dio-zero.jar -Djava.security.policy=config/gpio.policy com.diozero.sampleapps.PwmTest 12
 * Raspberry Pi BCM GPIO pins with hardware PWM support: 12 (phys 32, wPi 26), 13 (phys 33, wPi 23), 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
 */
public class PwmTest {
	private static final Logger logger = LogManager.getLogger(PwmTest.class);
	
	public static void main(String[] args) {
		if (args.length < 1) {
			logger.error("Usage: PWMTest <pin number>");
			System.exit(1);
		}
		
		int pin = Integer.parseInt(args[0]);
		try (PwmOutputDevice pwm = new PwmOutputDevice(pin)) {
			for (float f=0; f<1; f+=0.05) {
				logger.info("Setting value to " + f);
				pwm.setValue(f);
				SleepUtil.sleepSeconds(0.5);
			}
			logger.info("Done");
		} catch (IOException e) {
			logger.error("Error: " + e, e);
			e.printStackTrace();
		}
	}
}
