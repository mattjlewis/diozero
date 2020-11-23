package com.diozero.internal.provider.test;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     TestMcpAdcSpiDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import java.util.Random;

import org.junit.jupiter.api.Assertions;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.devices.McpAdc;
import com.diozero.internal.spi.DeviceFactoryInterface;

public class TestMcpAdcSpiDevice extends TestSpiDevice {
	private static final Random random = new Random();
	
	private static McpAdc.Type type;
	static {
		// Default to MCP3208
		setType(McpAdc.Type.MCP3208);
	}
	
	public static void setType(McpAdc.Type type) {
		TestMcpAdcSpiDevice.type = type;
	}

	public TestMcpAdcSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int chipSelect, int frequency, SpiClockMode spiClockMode, boolean lsbFirst) {
		super(key, deviceFactory, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}
	
	@Override
	public void write(byte[] txBuffer) throws RuntimeIOException {
		
	}
	
	@Override
	public void write(byte[] txBuffer, int index, int length) throws RuntimeIOException {
		
	}

	@Override
	public byte[] writeAndRead(byte[] txBuffer) throws RuntimeIOException {
		byte b = txBuffer[0];
		//out.put((byte) (0x10 | (differentialRead ? 0 : 0x08 ) | adcPin));
		//int pin = b & 0x07;
		//Logger.debug("Received read request for pin {}", Integer.valueOf(pin));
		b = txBuffer[1];
		Assertions.assertEquals(0, b);
		b = txBuffer[2];
		Assertions.assertEquals(0, b);
		
		int temp = random.nextInt(type.getRange());
		// FIXME Support MCP3301
		byte[] dst = new byte[3];
		dst[0] = (byte) 0;
		switch (type.name().substring(0, 5)) {
		case "MCP30":
			// Rx x0RRRRRR RRRRxxxx for the 30xx (10-bit unsigned)
			dst[1] = (byte) ((temp >> 4) & 0x3f);
			dst[2] = (byte) ((temp << 4) & 0xf0);
			break;
		case "MCP32":
			// Rx x0RRRRRR RRRRRRxx for the 32xx (12-bit unsigned)
			dst[1] = (byte) ((temp >> 6) & 0x3f);
			dst[2] = (byte) ((temp << 2) & 0xfc);
			break;
		case "MCP33":
			// Signed
			temp *= random.nextBoolean() ? 1 : -1;
			// Rx x0SRRRRR RRRRRRRx for the 33xx (13-bit signed)
			dst[1] = (byte) ((temp >> 7) & 0x3f);
			dst[2] = (byte) ((temp << 1) & 0xfe);
			break;
		}
		
		return dst;
	}
}
