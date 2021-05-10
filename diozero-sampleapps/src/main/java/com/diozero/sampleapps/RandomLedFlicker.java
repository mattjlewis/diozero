package com.diozero.sampleapps;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

import com.diozero.devices.PwmLed;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * LED random flicker using PWM. To run:
 * <ul>
 * <li>Built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.RandomLedFlicker 12}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.RandomLedFlicker 12}</li>
 * </ul>
 */
public class RandomLedFlicker {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <gpio>", RandomLedFlicker.class.getName());
			System.exit(1);
		}

		test(Integer.parseInt(args[0]));
	}

	private static void test(int pin) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		try (PwmLed led = new PwmLed(pin)) {
			DiozeroScheduler.getNonDaemonInstance().invokeAtFixedRate(random::nextFloat, led::setValue, 50, 50,
					TimeUnit.MILLISECONDS);
			SleepUtil.sleepSeconds(10);
		}
	}
}
