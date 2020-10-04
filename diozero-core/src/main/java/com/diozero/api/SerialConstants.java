package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SerialConstants.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

public interface SerialConstants {
	static final int DEFAULT_BAUD = 9600;

	/**
	 * The number of data bits to use per word.
	 */
	static enum DataBits {
		CS5, CS6, CS7, CS8;
	}
	static final DataBits DEFAULT_DATA_BITS = DataBits.CS8;

	/**
	 * The number of stop bits.
	 */
	static enum StopBits {
		ONE_STOP_BIT, TWO_STOP_BITS;
	}
	static final StopBits DEFAULT_STOP_BITS = StopBits.ONE_STOP_BIT;

	/**
	 * Specifies how error detection is carried out.
	 */
	static enum Parity {
		NO_PARITY, EVEN_PARITY, ODD_PARITY, MARK_PARITY, SPACE_PARITY;
	}
	static final Parity DEFAULT_PARITY = Parity.NO_PARITY;
}
