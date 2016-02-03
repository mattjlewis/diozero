package com.diozero.sampleapps;

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
