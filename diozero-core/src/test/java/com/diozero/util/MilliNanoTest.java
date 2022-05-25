package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MilliNanoTest.java
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

import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

@SuppressWarnings("static-method")
public class MilliNanoTest {
	@Test
	public void test() {
		double secs = 0.005123;
		
		long millis = Math.round(secs * SleepUtil.MS_IN_SEC);
		long nanos = Math.round(secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Long.valueOf(millis), Long.valueOf(nanos)));
		
		secs = 0.099789;
		
		millis = Math.round(secs * SleepUtil.MS_IN_SEC);
		nanos = Math.round(secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Long.valueOf(millis), Long.valueOf(nanos)));
		
		secs = 99.099789;
		
		millis = Math.round(secs * SleepUtil.MS_IN_SEC);
		nanos = Math.round(secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Long.valueOf(millis), Long.valueOf(nanos)));
	}
}
