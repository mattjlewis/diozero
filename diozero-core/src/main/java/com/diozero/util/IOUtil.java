package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     IOUtil.java  
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IOUtil {
	public static int getUShort(ByteBuffer buffer) {
		return buffer.getShort() & 0xffff;
	}

	/**
	 * Currently broken - see tests below
	 * @param data
	 * @param numBytes
	 * @param byteOrder
	 * @return
	 */
	private static int getInt(byte[] data, int numBytes, ByteOrder byteOrder) {
		int val = 0;
		for (int i = 0; i < numBytes; i++) {
			val |= (data[byteOrder == ByteOrder.LITTLE_ENDIAN ? numBytes - i - 1 : i] & 0xff) << (8
					* (numBytes - i - 1));
		}

		return val;
		// return buffer.getInt() & 0xffffffffL;
		/*-
		byte[] data = new byte[4];
		buffer.get(data);
		
		// Normal order:
		// ((data[0] << 8) & 0xFF00) | (data[1] & 0xFF);
		// Reverse order:
		// ((data[1] << 8) & 0xFF00) | (data[0] & 0xFF);
		
		long val = 0;
		for (int i=0; i<numBytes; i++) {
			val |= (data[order == ByteOrder.LITTLE_ENDIAN ? length-i-1 : i] & 0xff) << (8 * (length - i - 1));
		}
		
		return val;
		*/
	}

	public static void main(String[] args) {
		int value = 99999;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 4);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 4);
		value = -value;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 4);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 4);
		value = Integer.MAX_VALUE;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 4);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 4);
		value = Integer.MIN_VALUE;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 4);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 4);
		value = Short.MAX_VALUE;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 2);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 2);
		value = -value;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 2);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 2);
		value = Short.MAX_VALUE * 8;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 3);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 3);
		value = -value;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 3);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 3);
		// These should fail
		value = -value;
		testGetInt(ByteOrder.BIG_ENDIAN, value, 2);
		testGetInt(ByteOrder.LITTLE_ENDIAN, value, 2);
	}

	private static void testGetInt(ByteOrder byteOrder, int value, int byteLength) {
		if (byteLength < 1 || byteLength > 4) {
			System.out.println("Error: Invalid byte length " + byteLength);
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(byteLength);
		buffer.order(byteOrder);
		if (byteLength == 4) {
			buffer.putInt(value);
		} else {
			for (int i=0; i<byteLength; i++) {
				buffer.put((byte) (value << (8 * (byteLength - 1))));
			}
		}
		buffer.flip();

		byte[] data = new byte[byteLength];
		buffer.get(data);

		int converted = getInt(data, byteLength, byteOrder);

		if (converted == value) {
			System.out.println("Success: expected " + value + ", got " + converted + ", byte order: " + byteOrder
					+ ", byte length: " + byteLength);
		} else {
			System.out.println("Error: expected " + value + ", got " + converted + ", byte order: " + byteOrder
					+ ", byte length: " + byteLength);
		}
	}
}
