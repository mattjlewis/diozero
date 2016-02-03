package com.diozero;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.diozero.MCP3008;
import com.diozero.api.AnalogueInputDevice;
import com.diozero.util.SleepUtil;

/**
 * MCP3008 test using the test device factory
 */
public class MCP3008Test {
	private static final Logger logger = LogManager.getLogger(MCP3008Test.class);
	
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		int spi_chip_select = 0;
		int pin_number = 0;
		int iterations = 5;
		float voltage = 3.3f;

		try (MCP3008 mcp3008 = new MCP3008(0, spi_chip_select, voltage)) {
			//mcp3008.provisionAnalogueInputPin(pin_number);
			try (AnalogueInputDevice device = mcp3008.provisionAnalogueInputDevice(pin_number)) {
				for (int i=0; i<iterations; i++) {
					float v = mcp3008.getVoltage(pin_number);
					logger.info(String.format("Voltage: %.2f", Float.valueOf(v)));
					Assert.assertTrue("Voltage range", v >= 0 && v < voltage);
					float pin_val = device.getValue();
					logger.info(String.format("Raw val: %.2f", Float.valueOf(pin_val)));
					Assert.assertTrue("Voltage range", v >= 0 && v < voltage);
					SleepUtil.sleepMillis(100);
				}
			}
		} catch (IOException ioe) {
			logger.error("Error: " + ioe, ioe);
		}
	}
}
