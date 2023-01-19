package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SpiClockMode.java
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

/**
 * SPI clock mode determines the clock polarity and phase with respect to data.
 * See <a href=
 * "https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Clock_polarity_and_phase">
 * SPI clock polarity and phase</a>. ARM-based MCU use the following mode
 * numbers. Examine the device datasheet to understand the clock mode or modes
 * supported.
 *
 * <pre>
 * SPI  Clock Polarity Clock Phase Clock Edge
 * Mode (CPOL/CKP)     (CPHA)      (CKE/NCPHA)
 * 0    0              0           1
 * 1    0              1           0
 * 2    1              0           1
 * 3    1              1           0
 * </pre>
 *
 * Useful: https://elinux.org/images/1/1e/I2C-SPI-ELC-2020.pdf
 */
public enum SpiClockMode {
	// https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_numbers
	MODE_0(0), MODE_1(1), MODE_2(2), MODE_3(3);

	private byte mode;

	SpiClockMode(int mode) {
		this.mode = (byte) mode;
	}

	public byte getMode() {
		return mode;
	}

	public static SpiClockMode getByMode(int mode) {
		switch (mode) {
		case 0:
			return MODE_0;
		case 1:
			return MODE_1;
		case 2:
			return MODE_2;
		case 3:
			return MODE_3;
		default:
			return null;
		}
	}
}
