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

import com.diozero.LDR;
import com.diozero.MCP3008;
import com.diozero.util.SleepUtil;

public class LDRTest {
	private static final Logger logger = LogManager.getLogger(LDRTest.class);
	
	public static void main(String[] args) {
		if (args.length < 2) {
			logger.error("Usage: LDRTest <chip-select> <adc-pin>");
			System.exit(2);
		}
		int chip_select = Integer.parseInt(args[0]);
		int adc_pin = Integer.parseInt(args[1]);
		float vref = 3.3f;
		int r1 = 10_000;
		
		try (MCP3008 mcp3008 = new MCP3008(chip_select)) {
			try (LDR ldr = new LDR(mcp3008, adc_pin, vref, r1)) {
				while (true) {
					double lux = ldr.getLuminosity();
					logger.info(String.format("Lux: %.2f", Double.valueOf(lux)));
					SleepUtil.sleepSeconds(.5);
				}
			}
		} catch (IOException e) {
			logger.error("Error: " + e, e);
		}
	}
}
