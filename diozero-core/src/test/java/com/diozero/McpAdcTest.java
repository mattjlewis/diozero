package com.diozero;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.devices.McpAdc;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestMcpAdcSpiDevice;
import com.diozero.util.SleepUtil;

/**
 * Base class for testing the various MCP ADC types
 */
public abstract class McpAdcTest {
	protected McpAdc.Type type;

	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setSpiDeviceClass(TestMcpAdcSpiDevice.class);
	}
	
	public McpAdcTest(McpAdc.Type type) {
		this.type = type;
	}

	@BeforeEach
	public void setup() {
		Logger.info("setup(), type=" + type);
		TestMcpAdcSpiDevice.setType(type);
	}
	
	@Test
	public void test() {
		int spi_chip_select = 0;
		int pin_number = 1;
		int iterations = 20;
		float voltage = 3.3f;

		try (McpAdc adc = new McpAdc(type, spi_chip_select, voltage);
				AnalogInputDevice device = new AnalogInputDevice(adc, pin_number)) {
			for (int i=0; i<iterations; i++) {
				float unscaled_val = adc.getValue(pin_number);
				Logger.info("Value: {}", String.format("%.2f", Float.valueOf(unscaled_val)));
				if (type.isSigned()) {
					Assertions.assertTrue(unscaled_val >= -1 && unscaled_val < 1, "Unscaled range");
				} else {
					Assertions.assertTrue(unscaled_val >= 0 && unscaled_val < 1, "Unscaled range");
				}
				
				unscaled_val = device.getUnscaledValue();
				Logger.info("Unscaled value: {}", String.format("%.2f", Float.valueOf(unscaled_val)));
				if (type.isSigned()) {
					Assertions.assertTrue(unscaled_val >= -1 && unscaled_val < 1, "Unscaled range");
				} else {
					Assertions.assertTrue(unscaled_val >= 0 && unscaled_val < 1, "Unscaled range");
				}
				
				float scaled_val = device.getScaledValue();
				Logger.info("Scaled value: {}", String.format("%.2f", Float.valueOf(scaled_val)));
				if (type.isSigned()) {
					Assertions.assertTrue(scaled_val >= -voltage && scaled_val < voltage, "Scaled range");
				} else {
					Assertions.assertTrue(scaled_val >= 0 && scaled_val < voltage, "Scaled range");
				}
				
				SleepUtil.sleepMillis(100);
			}
		}
	}
}
