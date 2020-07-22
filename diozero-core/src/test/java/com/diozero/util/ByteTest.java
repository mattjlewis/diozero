package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     ByteTest.java  
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

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings({ "static-method", "boxing" })
public class ByteTest {
	@Test
	public void test() {
		byte b = -125;
		//byte b = (byte) 0b11110000;
		int i = b;
		
		byte new_value = (byte) ((b >> 4) & 0x0f);
		new_value |= b << 4;
		
		byte b2 = (byte) i;
		int i2 = b2 & 0xff;
		
		//sun.misc.HexDumpEncoder enc = new sun.misc.HexDumpEncoder();
		//String s = enc.encode(new byte[] { b, b2, new_value, (byte) (b >> 1), (byte) (b >>> 1), (byte) (b >> 2),
		//		(byte) (b >> 3), (byte) (b >> 4), (byte) (b >> 5), (byte) (b >>> 5) });
		
		System.out.format("b=%d, i=%d, b2=%d, i2=%d, new_value=%d%n", b, i, b2, i2, new_value);
		Hex.dumpByteArray(new byte[] { b, b2, new_value, (byte) (b >> 1), (byte) (b >>> 1), (byte) (b >> 2),
				(byte) (b >> 3), (byte) (b >> 4), (byte) (b >> 5), (byte) (b >>> 5) });
		
		b = (byte) 0xff;
		byte bit = 0;
		
		Assert.assertEquals(0, b & bit);
		Assert.assertTrue(BitManipulation.isBitSet(b, bit));
		
		int num_bytes = 35;
		byte[] bytes = new byte[num_bytes];
		Random rand = new Random();
		rand.nextBytes(bytes);
		//enc.encode(bytes, System.out);
		Hex.dumpByteArray(bytes);
	}
}
