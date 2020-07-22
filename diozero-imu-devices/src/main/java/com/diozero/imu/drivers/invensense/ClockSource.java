package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     ClockSource.java  
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


/* Clock sources. */
/**
 * CLKSEL	Clock Source
 * 0		Internal 8MHz oscillator
 * 1		PLL with X axis gyroscope reference
 * 2		PLL with Y axis gyroscope reference
 * 3		PLL with Z axis gyroscope reference
 * 4		PLL with external 32.768kHz reference
 * 5		PLL with external 19.2MHz reference
 * 6		Reserved
 * 7		Stops the clock and keeps the timing generator in reset
 */
public enum ClockSource {
	INV_CLK_INTERNAL((byte)0),
	INV_CLK_PLL((byte)1);

	private byte val;
	
	private ClockSource(byte val) {
		this.val = val;
	}
	
	public byte getVal() {
		return val;
	}
}
