package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ByteTest.java
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

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "static-method", "boxing" })
public class ByteTest {
	@Test
	public void test() {
		byte b = -125;
		// byte b = (byte) 0b11110000;
		int i = b;

		byte new_value = (byte) ((b >> 4) & 0x0f);
		new_value |= b << 4;

		byte b2 = (byte) i;
		int i2 = b2 & 0xff;

		// sun.misc.HexDumpEncoder enc = new sun.misc.HexDumpEncoder();
		// String s = enc.encode(new byte[] { b, b2, new_value, (byte) (b >> 1), (byte)
		// (b >>> 1), (byte) (b >> 2),
		// (byte) (b >> 3), (byte) (b >> 4), (byte) (b >> 5), (byte) (b >>> 5) });

		System.out.format("b=%d, i=%d, b2=%d, i2=%d, new_value=%d%n", b, i, b2, i2, new_value);
		Hex.dumpByteArray(new byte[] { b, b2, new_value, (byte) (b >> 1), (byte) (b >>> 1), (byte) (b >> 2),
				(byte) (b >> 3), (byte) (b >> 4), (byte) (b >> 5), (byte) (b >>> 5) });

		b = (byte) 0xff;
		byte bit = 0;

		Assertions.assertEquals(0, b & bit);
		Assertions.assertTrue(BitManipulation.isBitSet(b, bit));

		int num_bytes = 35;
		byte[] bytes = new byte[num_bytes];
		ThreadLocalRandom.current().nextBytes(bytes);
		// enc.encode(bytes, System.out);
		Hex.dumpByteArray(bytes);
	}

	@Test
	public void max30102IntUnpack() {
		byte[] b = { 0x15, (byte) 0xEC, (byte) 0xCD };
		int val = readUnsignedInt(b, 0);
		Assertions.assertEquals(126157, val);

		b = new byte[] { 0x15, (byte) 0xdd, 0x67 };
		val = readUnsignedInt(b, 0);
		Assertions.assertEquals(122215, val);
	}

	private static int readUnsignedInt(byte[] data, int pos) {
		// red_led = (d[0] << 16 | d[1] << 8 | d[2]) & 0x03FFFF
		// d[0]: 0x15 [21], d[1]: 0xEC [236], d[3]: 0xCD [205], red_led: 0x15ECCD =
		// 126157
		// d[0]: 0x15, d[1]: 0xEC, d[3]: 0xCD (0x15ECCD), red_led: 0x1ECCD
		// 0x15 [21] 0xdd [221] 0x67 [103] (0x15dd67):
		return (((data[pos * 3] & 0xff) << 16) | ((data[pos * 3 + 1] & 0xff) << 8) | (data[pos * 3 + 2] & 0xff))
				& 0x03FFFF;
	}
}
