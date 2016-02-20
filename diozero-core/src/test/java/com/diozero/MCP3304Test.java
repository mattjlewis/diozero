package com.diozero;

import org.junit.Assert;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogueInputDevice;
import com.diozero.internal.provider.test.TestMcpAdcSpiDevice;
import com.diozero.util.SleepUtil;

/**
 * MCP3008 test using the test device factory
 */
public class MCP3304Test {
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		int spi_chip_select = 0;
		int pin_number = 0;
		int iterations = 20;
		float voltage = 3.3f;

		TestMcpAdcSpiDevice.setType(McpAdc.MCP3304);
		try (McpAdc adc = new McpAdc(McpAdc.MCP3304, spi_chip_select);
				AnalogueInputDevice device = new AnalogueInputDevice(adc, pin_number, voltage)) {
			for (int i=0; i<iterations; i++) {
				float unscaled_val = adc.getValue(pin_number);
				Logger.info("Value: {}", String.format("%.2f", Float.valueOf(unscaled_val)));
				Assert.assertTrue("Unscaled range", unscaled_val >= -1 && unscaled_val < 1);
				
				unscaled_val = device.getUnscaledValue();
				Logger.info("Unscaled value: {}", String.format("%.2f", Float.valueOf(unscaled_val)));
				Assert.assertTrue("Unscaled range", unscaled_val >= -1 && unscaled_val < 1);
				
				float scaled_val = device.getScaledValue();
				Logger.info("Scaled value: {}", String.format("%.2f", Float.valueOf(scaled_val)));
				Assert.assertTrue("Scaled range", scaled_val >= -voltage && scaled_val < voltage);
				
				SleepUtil.sleepMillis(100);
			}
		}
	}
}
