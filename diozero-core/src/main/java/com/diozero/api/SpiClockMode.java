package com.diozero.api;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     SpiClockMode.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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
 * <pre>
 * SPI  Clock Polarity Clock Phase Clock Edge
 * Mode (CPOL/CKP)     (CPHA)      (CKE/NCPHA)
 * 0    0              0           1
 * 1    0              1           0
 * 2    1              0           1
 * 3    1              1           0
 * </pre>
 */
public enum SpiClockMode {
	// https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_numbers
	MODE_0((byte) 0), MODE_1((byte) 1), MODE_2((byte) 2), MODE_3((byte) 3);
	
	private byte mode;
	
	private SpiClockMode(byte mode) {
		this.mode = mode;
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
