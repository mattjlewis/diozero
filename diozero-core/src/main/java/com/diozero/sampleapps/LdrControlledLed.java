package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.LDR;
import com.diozero.MCP3008;
import com.diozero.PwmLed;
import com.diozero.util.SleepUtil;

/**
 * Control the brightness of an LED by the brightness of an LDR
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LdrControlledLed 0 3 18
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LdrControlledLed 0 3 18
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.LdrControlledLed 0 3 18
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.LdrControlledLed 0 3 18
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar com.diozero.sampleapps.LdrControlledLed 0 3 18
 */
public class LdrControlledLed {
	public static void main(String[] args) {
		if (args.length < 3) {
			Logger.error("Usage: {} <chip-select> <adc-pin> <led-pin>", LdrControlledLed.class.getName());
			System.exit(2);
		}
		int chip_select = Integer.parseInt(args[0]);
		int adc_pin = Integer.parseInt(args[1]);
		int led_pin = Integer.parseInt(args[2]);
		// TODO Add these to command line args
		float vref = 3.3f;
		int r1 = 10_000;
		
		test(chip_select, adc_pin, vref, r1, led_pin);
	}
	
	public static void test(int chipSelect, int pin, float vRef, int r1, int ledPin) {
		try (MCP3008 mcp3008 = new MCP3008(chipSelect); LDR ldr = new LDR(mcp3008, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
			// Detect variations of 5%
			ldr.addListener(.05f, (event) -> led.setValue(1-event.getScaledValue()));
			
			Logger.debug("Sleeping for 20s");
			SleepUtil.sleepSeconds(20);
		}
	}
}
