package com.diozero.sandpit;

import org.pmw.tinylog.Logger;

import com.diozero.util.SleepUtil;

/**
 * PCF8591 sample application.
 * To run:
 * <ul>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCF8591App 3}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCF8591App 3}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCF8591App 3}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCF8591App 3}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.PCF8591App 3}</li>
 * </ul>
 */
public class PCF8591App {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <adc_pin>", PCF8591App.class.getName());
			System.exit(2);
		}
		int adc_pin = Integer.parseInt(args[0]);
		test(adc_pin);
	}
	
	private static void test(int adcPin) {
		try (PCF8591 adc = new PCF8591()) {
			while (true) {
				float val = adc.getValue(adcPin);
				Logger.info(String.format("Pin %d value=%.2f, v=%.2f",
						Integer.valueOf(adcPin), Float.valueOf(val), Float.valueOf(val*3.3f)));
				
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
