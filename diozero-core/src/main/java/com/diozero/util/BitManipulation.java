package com.diozero.util;

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


public class BitManipulation {
	public static byte setBitValue(byte value, boolean on, int bit) {
		byte mask = getBitMask(bit);
		byte new_value = value;
		if (on) {
			new_value |= mask;
		} else {
			new_value &= ~mask;
		}
		
		return new_value;
	}
	
	public static byte getBitMask(int ... bits) {
		byte mask = 0;
		
		for (int bit : bits) {
			mask |= getBitMask(bit);
		}
		
		return mask;
	}
	
	public static byte getBitMask(int bit) {
		if (bit > 7) {
			throw new IllegalArgumentException("Bit (" + bit + ") out of range for byte");
		}
		
		return (byte)(1 << bit);
	}

	public static boolean isBitSet(byte value, byte bit) {
		return (value & getBitMask(bit)) != 0;
	}
}
