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

import com.diozero.McpAdc;
import com.diozero.TMP36;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * TMP36 temperature sensor test
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio10-0.3-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.TMP36Test MCP3208 0 1
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio11-0.3-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.TMP36Test MCP3208 0 1
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pi4j-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.TMP36Test MCP3208 0 1
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-wiringpi-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.TMP36Test MCP3208 0 1
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.TMP36Test MCP3208 0 1
 */
public class TMP36Test {
	private static final float DEFAULT_TEMPERATURE_OFFSET = 1.04f;
	private static final int ITERATIONS = 20;

	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <mcp-name> <chip-select> <adc-pin> [<temp-offset>]", TMP36Test.class.getName());
			System.exit(2);
		}
		McpAdc.Type type = McpAdc.Type.valueOf(args[0]);
		if (type == null) {
			Logger.error("Invalid MCP ADC type '{}'. Usage: {} <mcp-name> <spi-chip-select> <adc_pin>", args[0], McpAdcTest.class.getName());
			System.exit(2);
		}
		
		int chip_select = Integer.parseInt(args[1]);
		int adc_pin = Integer.parseInt(args[2]);
		float temp_offset = DEFAULT_TEMPERATURE_OFFSET;
		if (args.length > 2) {
			temp_offset = Float.parseFloat(args[2]);
		}
		float v_ref = 3.3f;
		test(type, chip_select, adc_pin, temp_offset, v_ref);
	}
	
	public static void test(McpAdc.Type type, int chipSelect, int pin, float tempOffset, float vRef) {
		try (McpAdc adc = new McpAdc(type, chipSelect);
				TMP36 tmp36 = new TMP36(adc, pin, tempOffset, vRef)) {
			for (int i=0; i<ITERATIONS; i++) {
				double tmp = tmp36.getTemperature();
				Logger.info("Temperature: {}", String.format("%.2f", Double.valueOf(tmp)));
				SleepUtil.sleepSeconds(.5);
			}
		} catch (RuntimeIOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		}
	}
}
