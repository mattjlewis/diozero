package com.diozero.sampleapps;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;

import com.diozero.PwmLed;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.RandomLedFlicker 18
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.RandomLedFlicker 18
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.RandomLedFlicker 18
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.RandomLedFlicker 18
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar com.diozero.sampleapps.RandomLedFlicker 18
 */
public class RandomLedFlicker {
	private static final Random RANDOM = new Random(System.nanoTime());
	
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <BCM pin number>", RandomLedFlicker.class.getName());
			System.exit(1);
		}
		
		test(Integer.parseInt(args[0]));
	}

	private static void test(int pin) {
		try (PwmLed led = new PwmLed(pin)) {
			DioZeroScheduler.getDaemonInstance().invokeAtFixedRate(RANDOM::nextFloat, led::setValue, 50, 50, TimeUnit.MILLISECONDS);
			SleepUtil.sleepSeconds(10);
		}
	}
}
