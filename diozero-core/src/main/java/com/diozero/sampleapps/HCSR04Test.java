package com.diozero.sampleapps;

import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.HCSR04;
import com.diozero.util.SleepUtil;

/**
 * 
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.HCSR04Test 23 24
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.HCSR04Test 23 24
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.HCSR04Test 23 24
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.HCSR04Test 23 24
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar -Djava.library.path=. com.diozero.sampleapps.HCSR04Test 23 24
 */
public class HCSR04Test {
	public static void main(String[] args) {
		if (args.length != 2) {
			Logger.error("Usage: HCSR04 <trigger GPIO> <echo GPIO>");
			System.exit(1);
		}
		int trigger_gpio = Integer.parseInt(args[0]);
		int echo_gpio = Integer.parseInt(args[1]);
		try (HCSR04 device = new HCSR04()) {
			device.init(trigger_gpio, echo_gpio);
			
			while (true) {
				Logger.info("Distance = {} cm", String.format("%.3f", Double.valueOf(device.getDistanceCm())));
				SleepUtil.sleepMillis(1000);
			}
		} catch (IOException ex) {
			Logger.error(ex, "I/O error with HC-SR04 device: {}", ex.getMessage());
		}
	}
}
