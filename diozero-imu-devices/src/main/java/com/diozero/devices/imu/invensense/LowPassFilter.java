package com.diozero.devices.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     LowPassFilter.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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


/* Filter configurations. */
public enum LowPassFilter {
	INV_FILTER_256HZ_NOLPF2((byte)0, 256),
	INV_FILTER_188HZ((byte)1, 188),
	INV_FILTER_98HZ((byte)2, 98),
	INV_FILTER_42HZ((byte)3, 42),
	INV_FILTER_20HZ((byte)4, 20),
	INV_FILTER_10HZ((byte)5, 10),
	INV_FILTER_5HZ((byte)6, 5),
	INV_FILTER_2100HZ_NOLPF((byte)7, 2100);
	
	private byte bit;
	private byte bitVal;
	private int freq;
	
	private LowPassFilter(byte bit, int freq) {
		this.bit = bit;
		bitVal = bit;
		this.freq = freq;
	}
	
	public byte getBit() {
		return bit;
	}
	
	public byte getBitVal() {
		return bitVal;
	}
	
	public int getFreq() {
		return freq;
	}
	
	public static LowPassFilter getForFrequency(int frequency) {
		LowPassFilter lpf;
		if (frequency >= LowPassFilter.INV_FILTER_188HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_188HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_98HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_98HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_42HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_42HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_20HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_20HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_10HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_10HZ;
		} else {
			lpf = LowPassFilter.INV_FILTER_5HZ;
		}
		
		return lpf;
	}
}
