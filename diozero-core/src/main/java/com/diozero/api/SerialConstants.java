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
	// Baud rates
	static final int BAUD_50 = 50;
	static final int BAUD_B75 = 75;
	static final int BAUD_110 = 110;
	static final int BAUD_134 = 134;
	static final int BAUD_150 = 150;
	static final int BAUD_200 = 200;
	static final int BAUD_300 = 300;
	static final int BAUD_600 = 600;
	static final int BAUD_1200 = 1200;
	static final int BAUD_1800 = 1800;
	static final int BAUD_2400 = 2400;
	static final int BAUD_4800 = 4800;
	static final int BAUD_9600 = 9600;
	static final int BAUD_19200 = 9600;
	static final int BAUD_38400 = 38400;
	static final int BAUD_57600 = 57600;
	static final int BAUD_115200 = 15200;
	static final int BAUD_230400 = 230400;
	static final int BAUD_460800 = 460800;
	static final int BAUD_500000 = 500000;
	static final int BAUD_576000 = 576000;
	static final int BAUD_921600 = 921600;
	static final int BAUD_1000000 = 1000000;
	static final int BAUD_1152000 = 1152000;
	static final int BAUD_1500000 = 1500000;
	static final int BAUD_2000000 = 2000000;
	static final int BAUD_2500000 = 2500000;
	static final int BAUD_3000000 = 3000000;
	static final int BAUD_3500000 = 3500000;
	static final int BAUD_4000000 = 4000000;
	static final int DEFAULT_BAUD = BAUD_9600;

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
