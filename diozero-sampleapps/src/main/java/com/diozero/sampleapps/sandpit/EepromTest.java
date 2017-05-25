package com.diozero.sampleapps.sandpit;

/*
 * #%L
 * Device I/O Zero - Sample applications
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import com.diozero.sandpit.Eeprom24LC512;

public class EepromTest {
	public static void main(String[] args) {
		try (Eeprom24LC512 eeprom = new Eeprom24LC512(2)) {
			for (int address = 0; address<2; address++) {
				System.out.println("0x" + Integer.toHexString(eeprom.randomRead(address) & 0xff));
				for (int data=0; data<256; data++) {
					eeprom.writeByte(address, data);
					System.out.print("0x" + Integer.toHexString(eeprom.randomRead(address) & 0xff) + " ");
				}
				System.out.println();
			}
			
			String text_to_write = "Hello World";
			int address = 0;
			for (byte b : text_to_write.getBytes()) {
				System.out.println("Writing '" + ((char) b) + "'");
				eeprom.writeByte(address++, b);
			}
			byte b = eeprom.randomRead(0);
			System.out.println("Read '" + ((char) b) + "'");
			for (int i=0; i<text_to_write.length()-1; i++) {
				b = eeprom.currentAddressRead();
				System.out.println("Read '" + ((char) b) + "'");
			}
			
			System.out.println("Writing '" + text_to_write + "'");
			eeprom.pageWrite(0, text_to_write.getBytes());
			b = eeprom.randomRead(0);
			System.out.println("Read '" + ((char) b) + "'");
			for (int i=0; i<text_to_write.length()-1; i++) {
				b = eeprom.currentAddressRead();
				System.out.println("Read '" + ((char) b) + "'");
			}
			
			byte[] data = eeprom.sequentialRead(0, text_to_write.length());
			System.out.println("read " + data.length + " bytes");
			String text = new String(data);
			System.out.println("Read '" + text + "'");
		}
	}
}
