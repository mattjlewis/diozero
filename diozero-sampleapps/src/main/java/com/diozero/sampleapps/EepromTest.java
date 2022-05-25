package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     EepromTest.java
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

import java.util.Arrays;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.devices.McpEeprom;

public class EepromTest {
	private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, "
			+ "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
			+ "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis "
			+ "aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla "
			+ "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia "
			+ "deserunt mollit anim id est laborum.";

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <i2c-controller> [device-address]", EepromTest.class.getName());
			System.exit(1);
		}

		int controller = Integer.parseInt(args[0]);
		int device_address = McpEeprom.DEFAULT_ADDRESS;
		if (args.length > 1) {
			device_address = Integer.parseInt(args[1]);
		}

		// test(controller, 0x04);
		// test(controller, 0x05);
		test(controller, device_address);
	}

	private static void test(int controller, int deviceAddress) {
		try (McpEeprom eeprom = new McpEeprom(controller, deviceAddress, McpEeprom.Type.MCP_24xx512)) {
			byte d = eeprom.readByte(0);
			Logger.info("Read " + (d & 0xff) + " from address 0");
			// Validate write byte and random read
			for (int address = 0; address < 2; address++) {
				Logger.info("Testing write and read for memory address: 0x{}", Integer.valueOf(address));
				for (int data = 0; data < 256; data++) {
					eeprom.writeByte(address, data);
					int data_read = eeprom.readByte(address) & 0xff;
					if (data_read != data) {
						Logger.error("For address 0x{} expected 0x{}, read 0x{}", Integer.toHexString(address),
								Integer.toHexString(data), Integer.toHexString(data_read));
					}
				}
			}

			// Validate write byte and current address read
			String text_to_write = "Hello World";
			int address = 0x10;
			for (byte b : text_to_write.getBytes()) {
				Logger.debug("Writing '" + ((char) b) + "'");
				eeprom.writeByte(address++, b);
			}
			address = 0x10;
			byte b = eeprom.readByte(address);
			Logger.debug("Read '" + ((char) b) + "'");
			for (int i = 1; i < text_to_write.length(); i++) {
				b = eeprom.readCurrentAddress();
				Logger.debug("Read '" + ((char) b) + "'");
				if (((char) b) != text_to_write.charAt(i)) {
					Logger.error("Error expected '{}', read '{}'", Character.valueOf(text_to_write.charAt(i)),
							Character.valueOf((char) b));
				}
			}

			// Test writing more that a page size
			address = 0x1000;
			Logger.debug("Writing '" + LOREM_IPSUM + "'");
			eeprom.writeBytes(address, LOREM_IPSUM.getBytes());
			byte[] data = eeprom.readBytes(address, LOREM_IPSUM.length());
			Logger.debug("read " + data.length + " bytes");
			String text = new String(data);
			Logger.debug("Read '" + text + "'");
			if (!Arrays.equals(LOREM_IPSUM.getBytes(), data)) {
				Logger.error("Expected to read Lorem Ipsum text");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e);
		}
	}
}
