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

import com.diozero.MCP3008;
import com.diozero.TMP36;
import com.diozero.util.SleepUtil;

/**
 * TMP36 temperature sensor test
 * To run:
 * (Pi4j):				sudo java -classpath dio-zero.jar:pi4j-core.jar com.diozero.TMP36 0 1
 * (JDK Device I/O):	sudo java -classpath dio-zero.jar -Djava.security.policy=config/gpio.policy com.diozero.TMP36 0 1
 */
public class TMP36Test {
	private static final Logger logger = LogManager.getLogger(TMP36Test.class);
	
	private static final double DEFAULT_TEMPERATURE_OFFSET = 1.04f;

	public static void main(String[] args) {
		if (args.length < 2) {
			logger.error("Usage: TMP36Test <chip-select> <adc-pin> [<temp-offset>]");
			System.exit(2);
		}
		int chip_select = Integer.parseInt(args[0]);
		int adc_pin = Integer.parseInt(args[1]);
		double temp_offset = DEFAULT_TEMPERATURE_OFFSET;
		if (args.length > 2) {
			temp_offset = Double.parseDouble(args[2]);
		}

		try (MCP3008 mcp3008 = new MCP3008(chip_select)) {
			try (TMP36 tmp36 = new TMP36(mcp3008, adc_pin, temp_offset)) {
				while (true) {
					double tmp = tmp36.getTemperature();
					logger.info(String.format("Temperature: %.2f", Double.valueOf(tmp)));
					SleepUtil.sleepSeconds(1);
				}
			}
		} catch (IOException ioe) {
			logger.error("Error: " + ioe, ioe);
		}
	}

}
