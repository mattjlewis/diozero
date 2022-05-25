package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SleepUtil.java
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

import com.diozero.api.RuntimeInterruptedException;

public class SleepUtil {
	public static final int MS_IN_SEC = 1000;
	public static final long US_IN_SEC = MS_IN_SEC * 1000;
	public static final long NS_IN_SEC = US_IN_SEC * 1000;

	public static final int NS_IN_US = 1000;
	public static final int US_IN_MS = 1000;
	public static final long NS_IN_MS = NS_IN_US * US_IN_MS;

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
		sleepMillis((long) (secs * MS_IN_SEC));
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
			Thread.currentThread().interrupt();
			throw new RuntimeInterruptedException(e);
		}
	}

	/**
	 * Busy sleep for the specified number of nanoseconds. It is the caller's
	 * responsibility to factor in any delays associated with calling this method -
	 * this could be as much as 1.5 microseconds on a Raspberry Pi 4.
	 *
	 * Warning - this will consume 100% of one core, use with caution and only with
	 * small delays.
	 *
	 * @param nanos The period to delay for
	 */
	public static void busySleep(final long nanos) {
		final long start_time = System.nanoTime();
		do {
			// nop
			// In Java 9+ use Thread.onSpinWait()?
		} while ((System.nanoTime() - start_time) < nanos);
	}

	/**
	 * Invoke the C nanosleep function via JNI. Note that the accuracy of the sleep
	 * is not guaranteed - the delta could be as much as 30 microseconds given the
	 * additional JNI overhead.
	 *
	 * Note - you must ensure that the diozero-system-utils library has been loaded
	 * via {@link com.diozero.util.LibraryLoader#loadSystemUtils loadSystemUtils()}
	 * prior to calling this method. The diozero APIs will do this on behalf of the
	 * application - this note is present for situations where such APIs have not
	 * yet been invoked.
	 *
	 * See C <a href=
	 * "https://man7.org/linux/man-pages/man2/nanosleep.2.html">nanosleep</a> man
	 * pages.
	 *
	 * @param seconds seconds to sleep for
	 * @param nanos   additional nanoseconds to sleep for; must be in the range 0 to
	 *                999999999
	 * @return the remaining nanoseconds if interrupted
	 */
	public static native long sleepNanos(final int seconds, final long nanos);
}
