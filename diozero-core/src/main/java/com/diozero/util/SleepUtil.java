package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SleepUtil.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.api.RuntimeInterruptedException;

public class SleepUtil {
	public static final int MS_IN_SEC = 1000;
	public static final long US_IN_SEC = MS_IN_SEC * 1000;
	public static final long NS_IN_SEC = US_IN_SEC * 1000;

	public static final int NS_IN_US = 1000;
	public static final int US_IN_MS = 1000;
	public static final long NS_IN_MS = NS_IN_US * US_IN_MS;

	private static final long BUSY_SLEEP_FUDGE_FACTOR_NS = 1_500;

	/**
	 * Sleep for the specific number of seconds
	 * 
	 * @param secs Number of seconds to sleep for
	 * @throws RuntimeInterruptedException if interrupted
	 */
	public static void sleepSeconds(final int secs) throws RuntimeInterruptedException {
		sleepMillis(secs * MS_IN_SEC);
	}

	/**
	 * Sleep for the specific number of seconds
	 * 
	 * @param secs Number of seconds to sleep for
	 * @throws RuntimeInterruptedException if interrupted
	 */
	public static void sleepSeconds(final double secs) throws RuntimeInterruptedException {
		long millis = (long) (secs * MS_IN_SEC);
		sleepMillis(millis);
	}

	/**
	 * Sleep for the specific number of milliseconds
	 * 
	 * @param millis Number of milliseconds to sleep for
	 * @throws RuntimeInterruptedException if interrupted
	 */
	public static void sleepMillis(final long millis) throws RuntimeInterruptedException {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeInterruptedException(e);
		}
	}

	public static void busySleep(final long nanos) {
		final long start_time = System.nanoTime();
		do {
			// nop
		} while ((System.nanoTime() - start_time) < (nanos - BUSY_SLEEP_FUDGE_FACTOR_NS));
	}

	private static native long sleepNanos(final int seconds, final long nanos);
}
