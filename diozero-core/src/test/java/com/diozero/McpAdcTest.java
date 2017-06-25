package com.diozero;

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
import org.junit.*;
import org.pmw.tinylog.Logger;

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

	@BeforeClass
	public static void beforeClass() {
		TestDeviceFactory.setSpiDeviceClass(TestMcpAdcSpiDevice.class);
	}
	
	public McpAdcTest(McpAdc.Type type) {
		this.type = type;
	}

	@Before
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
					Assert.assertTrue("Unscaled range", unscaled_val >= -1 && unscaled_val < 1);
				} else {
					Assert.assertTrue("Unscaled range", unscaled_val >= 0 && unscaled_val < 1);
				}
				
				unscaled_val = device.getUnscaledValue();
				Logger.info("Unscaled value: {}", String.format("%.2f", Float.valueOf(unscaled_val)));
				if (type.isSigned()) {
					Assert.assertTrue("Unscaled range", unscaled_val >= -1 && unscaled_val < 1);
				} else {
					Assert.assertTrue("Unscaled range", unscaled_val >= 0 && unscaled_val < 1);
				}
				
				float scaled_val = device.getScaledValue();
				Logger.info("Scaled value: {}", String.format("%.2f", Float.valueOf(scaled_val)));
				if (type.isSigned()) {
					Assert.assertTrue("Scaled range", scaled_val >= -voltage && scaled_val < voltage);
				} else {
					Assert.assertTrue("Scaled range", scaled_val >= 0 && scaled_val < voltage);
				}
				
				SleepUtil.sleepMillis(100);
			}
		}
	}
}
