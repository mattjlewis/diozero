package com.diozero.sampleapps;

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
