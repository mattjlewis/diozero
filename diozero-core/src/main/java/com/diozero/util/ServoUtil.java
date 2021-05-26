package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ServoUtil.java
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

public class ServoUtil {
	public static double calcPulseMsPerBit(int pwmFrequency, int range) {
		return (1000.0 / pwmFrequency) / range;
	}

	public static int calcServoPulseBits(double pulseWidthMs, double pulseMsPerBit) {
		return (int) Math.floor(pulseWidthMs / pulseMsPerBit);
	}

	/**
	 * Calculate the number of bits required for the specified pulse width
	 * (milliseconds)
	 * 
	 * @param pulseWidthMs The required pulse width value
	 * @param pwmFrequency The servo driver PWM Frequency
	 * @param range        The servo driver range, i.e. if 12 bit then this would be
	 *                     4096
	 * @return Relative servo driver duty cycle value required to set the requested
	 *         pulse width
	 */
	public static int calcServoPulseBits(double pulseWidthMs, int pwmFrequency, int range) {
		return (int) Math.floor(pulseWidthMs / calcPulseMsPerBit(pwmFrequency, range));
	}
}
