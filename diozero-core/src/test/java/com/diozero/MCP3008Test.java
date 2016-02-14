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

import org.junit.Assert;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogueInputDevice;
import com.diozero.util.SleepUtil;

/**
 * MCP3008 test using the test device factory
 */
public class MCP3008Test {
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		int spi_chip_select = 0;
		int pin_number = 0;
		int iterations = 5;
		float voltage = 3.3f;

		try (MCP3008 mcp3008 = new MCP3008(0, spi_chip_select, voltage)) {
			//mcp3008.provisionAnalogueInputPin(pin_number);
			try (AnalogueInputDevice device = new AnalogueInputDevice(mcp3008, pin_number, voltage)) {
				for (int i=0; i<iterations; i++) {
					float v = mcp3008.getVoltage(pin_number);
					Logger.info("Voltage: {}", String.format("%.2f", Float.valueOf(v)));
					Assert.assertTrue("Voltage range", v >= 0 && v < voltage);
					float pin_val = device.getUnscaledValue();
					Logger.info("Raw val: {}", String.format("%.2f", Float.valueOf(pin_val)));
					Assert.assertTrue("Voltage range", v >= 0 && v < voltage);
					SleepUtil.sleepMillis(100);
				}
			}
		}
	}
}
