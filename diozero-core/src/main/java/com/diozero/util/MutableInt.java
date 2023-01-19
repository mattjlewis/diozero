package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MutableInt.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

public class MutableInt {
	private int value;

	public MutableInt() {
		this(0);
	}

	public MutableInt(final int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public boolean isBitSet(final int bit) {
		return isBitSet(value, bit);
	}

	public MutableInt setValue(final int value) {
		this.value = value;
		return this;
	}

	public MutableInt setBit(final int bit) {
		value = setBitValue(value, bit, true);
		return this;
	}

	public MutableInt unsetBit(final int bit) {
		value = setBitValue(value, bit, false);
		return this;
	}

	public MutableInt setBitValue(final int bit, final boolean on) {
		value = setBitValue(value, bit, on);
		return this;
	}

	/**
	 * Update only the bits as specified by mask with the specified bits
	 *
	 * @param mask a bit mask with 1s specifying the bits to update
	 * @param bits the new bits to apply to the value
	 * @return this object
	 */
	public MutableInt updateWithMaskedData(final int mask, final int bits) {
		value = updateValueWithMaskedData(value, mask, bits);
		return this;
	}

	public boolean equals(final int b) {
		return value == b;
	}

	// Static methods

	public static int setBitValue(final int value, final int bit, final boolean on) {
		int mask = getBitMask(bit);
		int new_value = value;
		if (on) {
			new_value |= mask;
		} else {
			new_value &= ~mask;
		}

		return new_value;
	}

	public static int getBitMask(final int... bits) {
		int mask = 0;

		for (int bit : bits) {
			mask |= getBitMask(bit);
		}

		return mask;
	}

	public static int getBitMask(final int bit) {
		if (bit > 31) {
			throw new IllegalArgumentException("Bit (" + bit + ") out of range for int");
		}

		return 1 << bit;
	}

	public static boolean isBitSet(final int value, final int bit) {
		return (value & getBitMask(bit)) != 0;
	}

	/**
	 * Update only the bits as specified by mask with the specified bits
	 *
	 * @param value the value to update
	 * @param mask  a bit mask with 1s specifying the bits to update
	 * @param data  the new bits to apply to the value
	 * @return the updated value
	 */
	public static int updateValueWithMaskedData(final int value, final int mask, final int data) {
		return (value & ~mask) | (data & mask);
	}
}
