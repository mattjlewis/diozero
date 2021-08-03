package com.diozero.internal.provider.firmata.adapter;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     DataEncodeTest.java
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

import org.junit.jupiter.api.Assertions;

import com.diozero.util.Hex;

public class DataEncodeTest {
	public static void main(String[] args) {
		test(255);
		test(16384 - 1);
		test(Short.MAX_VALUE);
		test(268435456 - 1);
		test(Integer.MAX_VALUE);
		test(0xffffffff);
	}

	static void test(int val) {
		byte[] enc_data = FirmataProtocol.encodeValue(val);
		int dec_val = FirmataProtocol.decodeValue(enc_data);
		if (dec_val != val) {
			System.out.println("Error with encodeValue, expected " + val + " got " + dec_val);
			System.out.println("enc_data:");
			Hex.dumpByteArray(enc_data);
			System.out.println("val: " + val + ", dec_val: " + dec_val);
		}
		Assertions.assertEquals(val, dec_val);

		int val_array_len;
		if (val >= 0 && val < 256) { // 2^8
			val_array_len = 1;
		} else if (val >= 0 && val < 65536) { // 2^16
			val_array_len = 2;
		} else if (val >= 0 && val < 16777216) { // 2^24
			val_array_len = 3;
		} else {
			val_array_len = 4;
		}
		byte[] val_array = new byte[val_array_len];
		for (int i = 0; i < val_array.length; i++) {
			val_array[i] = (byte) ((val >> (8 * i)) & 0xff);
		}
		enc_data = FirmataProtocol.to7BitArray(val_array);
		byte[] val_array2 = FirmataProtocol.from7BitArray(enc_data);
		dec_val = 0;
		for (int i = 0; i < val_array2.length; i++) {
			dec_val |= (val_array2[i] & 0xff) << (8 * i);
		}
		if (dec_val != val) {
			System.out.println("Error with from7BitArray, val: " + val + ", dec_val: " + dec_val);
			System.out.println("val_array:");
			Hex.dumpByteArray(val_array);
			System.out.println("enc_data:");
			Hex.dumpByteArray(enc_data);
			System.out.println("val_array2:");
			Hex.dumpByteArray(val_array2);
		}
		Assertions.assertEquals(val, dec_val);
	}
}
