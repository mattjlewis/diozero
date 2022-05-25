package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ColourUtil.java
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

public class ColourUtil {
	/*
	 * Each pixel has 16-bit data.
	 * Three sub-pixels for colour A, B and C have 5 bits, 6 bits and 5 bits respectively.
	 */
	public static short createColour565(byte red, byte green, byte blue) {
		// 65k format 1 in normal order (ABC = RGB)
		// (2 bytes): MSB C4C3C2C1C0B5B4B3, LSB B2B1B0A4A3A2A1A0
		//            MSB B4B3B2B1B0G5G4G3, LSB G2G1G0R4R3R2R1R0
		int colour = blue & 0b11111;
		colour |= (green & 0b111111) << 5;
		colour |= (red & 0b11111) << 11;
		/*
		 * buf[i] = r & 0xF8 | g >> 5
		 * buf[i + 1] = g << 5 & 0xE0 | b >> 3
		 */
		return (short) colour;
	}
}
