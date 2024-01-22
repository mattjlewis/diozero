package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MutableByte.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

public class MutableByte {
	private byte value;

	public MutableByte() {
		this((byte) 0);
	}

	public MutableByte(final byte value) {
		this.value = value;
	}

	public byte getValue() {
		return value;
	}

	public boolean isBitSet(final int bit) {
		return BitManipulation.isBitSet(value, bit);
	}

	public MutableByte setValue(final byte value) {
		this.value = value;
		return this;
	}

	public MutableByte setBit(final byte bit) {
		value = BitManipulation.setBitValue(value, bit, true);
		return this;
	}

	public MutableByte unsetBit(final byte bit) {
		value = BitManipulation.setBitValue(value, bit, false);
		return this;
	}

	public MutableByte setBitValue(final int bit, final boolean on) {
		value = BitManipulation.setBitValue(value, bit, on);
		return this;
	}

	/**
	 * Update only the bits as specified by mask with the specified bits
	 *
	 * @param mask a bit mask with 1s specifying the bits to update
	 * @param data the new bits to apply to the value
	 * @return this object
	 */
	public MutableByte updateWithMaskedData(final short mask, final byte data) {
		value = BitManipulation.updateValueWithMaskedData(value, mask, data);
		return this;
	}

	public boolean equals(final byte b) {
		return value == b;
	}
}
