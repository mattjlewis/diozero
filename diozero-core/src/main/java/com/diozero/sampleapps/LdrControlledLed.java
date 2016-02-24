package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.LDR;
import com.diozero.McpAdc;
import com.diozero.PwmLed;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Control the brightness of an LED by the brightness of an LDR
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio10-0.3-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LdrControlledLed MCP3208 0 3 12
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio11-0.3-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LdrControlledLed MCP3208 0 3 12
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pi4j-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.LdrControlledLed MCP3208 0 3 12
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-wiringpi-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.LdrControlledLed MCP3208 0 3 12
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.LdrControlledLed MCP3208 0 3 12
 */
public class LdrControlledLed {
	public static void main(String[] args) {
		if (args.length < 3) {
			Logger.error("Usage: {} <mcp-name> <chip-select> <adc-pin> <led-pin>", LdrControlledLed.class.getName());
			System.exit(2);
		}
		McpAdc.Type type = McpAdc.Type.valueOf(args[0]);
		if (type == null) {
			Logger.error("Invalid MCP ADC type '{}'. Usage: {} <mcp-name> <spi-chip-select> <adc_pin>", args[0], McpAdcTest.class.getName());
			System.exit(2);
		}
		
		int chip_select = Integer.parseInt(args[1]);
		int adc_pin = Integer.parseInt(args[2]);
		int led_pin = Integer.parseInt(args[3]);
		// TODO Add these to command line args
		float vref = 3.3f;
		int r1 = 10_000;
		
		test(type, chip_select, adc_pin, vref, r1, led_pin);
	}
	
	public static void test(McpAdc.Type type, int chipSelect, int pin, float vRef, int r1, int ledPin) {
		try (McpAdc adc = new McpAdc(type, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
			// Detect variations of 5%, poll every 20ms
			ldr.addListener((event) -> led.setValue(1-event.getUnscaledValue()), .05f, 20);
			
			Logger.debug("Sleeping for 20s");
			SleepUtil.sleepSeconds(20);
		} catch (RuntimeIOException ex) {
			Logger.error(ex, "I/O error in LdrControlledLed: {}", ex.getMessage());
		}
	}
}
