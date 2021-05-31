package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BitManipulation.java
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

public class BitManipulation {
	public static byte setBitValue(final byte value, final boolean on, final int bit) {
		byte mask = getBitMask(bit);
		byte new_value = value;
		if (on) {
			new_value |= mask;
		} else {
			new_value &= ~mask;
		}

		return new_value;
	}

	public static byte getBitMask(final int... bits) {
		byte mask = 0;

		for (int bit : bits) {
			mask |= getBitMask(bit);
		}

		return mask;
	}

	public static byte getBitMask(final int bit) {
		if (bit > 7) {
			throw new IllegalArgumentException("Bit (" + bit + ") out of range for byte");
		}

		return (byte) (1 << bit);
	}

	public static boolean isBitSet(final byte value, final int bit) {
		return (value & getBitMask(bit)) != 0;
	}

	public static byte setBits(final byte value, final short mask, final byte position, final byte data) {
		if (position == 0) {
			return (byte) ((value & ~mask) | (data & mask));
		}
		return (byte) ((value & ~mask) | ((data << position) & mask));
	}

	public static int bytesToWord(final int msb, final int lsb, final boolean isSigned) {
		if (isSigned) {
			return (msb << 8) | (lsb & 0xff); // keep the sign of msb but not of lsb
		}
		return ((msb & 0xff) << 8) | (lsb & 0xff);
	}

	public static byte reverseByte(final byte value) {
		short s = (short) (value & 0xff);
		short y = 0;
		for (int position = 8 - 1; position >= 0; position--) {
			y += ((s & 1) << position);
			s >>= 1;
		}
		return (byte) (y & 0xff);
	}

	public static short reverseShort(final short value) {
		int i = (short) (value & 0xffff);
		int y = 0;
		for (int position = 16 - 1; position >= 0; position--) {
			y += ((i & 1) << position);
			i >>= 1;
		}
		return (short) (y & 0xffff);
	}
}
