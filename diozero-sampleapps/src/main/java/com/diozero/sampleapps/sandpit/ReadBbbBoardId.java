package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     ReadBbbBoardId.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import com.diozero.api.I2CDevice;

public class ReadBbbBoardId {
	public static void main(String[] args) {
		try (I2CDevice device = I2CDevice.builder(0x50).setController(0).build()) {
			byte[] address = { (byte) 0, 0 };
			device.writeBytes(address);
			for (int i=0; i<4; i++) {
				System.out.println("read 0x" + Integer.toHexString(0xff & device.readByte()));
			}
			
			device.writeBytes(address);
			for (int i=0; i<4; i++) {
				System.out.println("read 0x" + Integer.toHexString(0xff & device.readByte()));
			}
			
			device.writeBytes(address);
			byte[] eeprom_data = device.readBytes(28);
			for (int i=0; i<4; i++) {
				System.out.println("Header[" + i + "]: 0x" + Integer.toHexString(0xFF & eeprom_data[i]));
			}
			String board_name = new String(eeprom_data, 4, 8);
			System.out.println("Board Name: " + board_name);
			String version = new String(eeprom_data, 4+8, 4);
			System.out.println("Version: " + version);
			String serial_num = new String(eeprom_data, 4+8+4, 12);
			System.out.println("Serial Number: " + serial_num);
		}
	}
}
