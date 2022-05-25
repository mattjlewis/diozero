package com.diozero.internal;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     CleanupTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiDeviceInterface;
import com.diozero.devices.LED;
import com.diozero.devices.McpAdc;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestI2CDevice;
import com.diozero.internal.provider.test.TestMcpAdcSpiDevice;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.Diozero;

@SuppressWarnings("static-method")
public class CleanupTest {
	@BeforeAll
	public static void beforeAll() {
		TestMcpAdcSpiDevice.setType(McpAdc.Type.MCP3008);
		TestDeviceFactory.setI2CDeviceClass(TestI2CDevice.class);
		TestDeviceFactory.setSpiDeviceClass(TestMcpAdcSpiDevice.class);
	}

	@Test
	public void test() {
		TestDeviceFactory tdf = (TestDeviceFactory) DeviceFactoryHelper.getNativeDeviceFactory();
		try {
			DeviceStates ds = tdf.getDeviceStates();
			Assertions.assertTrue(ds.size() == 0);

			try (I2CDeviceInterface device = tdf.provisionI2CDevice(0, 0, I2CConstants.AddressSize.SIZE_7)) {
				byte[] buf = new byte[5];
				device.readI2CBlockData(0, buf);
				Assertions.assertTrue(ds.size() == 1);
				device.close();
				Assertions.assertTrue(ds.size() == 0);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
			}
			// Check the log for the above - make sure there is a warning about closing
			// already closed device

			Assertions.assertTrue(ds.size() == 0);
			try (SpiDeviceInterface device = tdf.provisionSpiDevice(0, 0, 0, SpiClockMode.MODE_0, false)) {
				byte[] tx = new byte[3];
				tx[0] = (byte) (0x10 | (false ? 0 : 0x08) | 1);
				tx[1] = (byte) 0;
				tx[2] = (byte) 0;
				device.writeAndRead(tx);
				Assertions.assertTrue(ds.size() == 1);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
			}

			Assertions.assertTrue(ds.size() == 0);
		} finally {
			Diozero.shutdown();
			Assertions.assertTrue(tdf.isShutdown());
			Assertions.assertTrue(tdf.isClosed());
			tdf.close();
		}

		try (LED led = new LED(10)) {
			Assertions.assertTrue(tdf.isStarted());
			Assertions.assertFalse(tdf.isShutdown());
			Assertions.assertFalse(tdf.isClosed());

			led.on();
			Assertions.assertTrue(led.isOn());
		}
	}
}
