package com.diozero.sampleapps;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.MCP3008;
import com.diozero.util.SleepUtil;

/**
 * MCP3008 test application
 * To run:
 * (Pi4j):				sudo java -classpath dio-zero.jar:pi4j-core.jar com.diozero.sampleapps.MCP3008Test 0 0
 * (JDK Device I/O):	sudo java -classpath dio-zero.jar -Djava.security.policy=config/gpio.policy com.diozero.sampleapps.MCP3008Test 0 0
 */
public class MCP3008Test {
	private static final Logger logger = LogManager.getLogger(MCP3008Test.class);
	
	public static void main(String[] args) {
		if (args.length < 2) {
			logger.error("Usage: MCP3008 <spi-chip-select> <adc_pin>");
			System.exit(2);
		}
		int spi_chip_select = Integer.parseInt(args[0]);
		int adc_pin = Integer.parseInt(args[1]);

		try (MCP3008 mcp3008 = new MCP3008(spi_chip_select)) {
			while (true) {
				float v = mcp3008.getVoltage(adc_pin);
				logger.info("Voltage: %.2f", Float.valueOf(v));
				SleepUtil.sleepMillis(1000);
			}
		} catch (IOException ioe) {
			logger.error("Error: " + ioe, ioe);
		}
	}
}
