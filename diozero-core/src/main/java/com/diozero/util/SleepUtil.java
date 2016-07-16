package com.diozero.util;

import java.util.concurrent.TimeUnit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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


public class SleepUtil {
	public static final int MS_IN_SEC = 1000;
	public static final long US_IN_SEC = MS_IN_SEC * 1000;
	public static final long NS_IN_SEC = US_IN_SEC * 1000;
	
	public static final int NS_IN_US = 1000;
	public static final int US_IN_MS = 1000;
	public static final long NS_IN_MS = NS_IN_US * US_IN_MS;
	
	/**
	 * Sleep for the specific number of seconds
	 * @param secs Number of seconds to sleep for
	 */
	public static void sleepSeconds(int secs) {
		sleepMillis(TimeUnit.SECONDS.toMillis(secs));
	}
	
	/**
	 * Sleep for the specific number of seconds
	 * @param secs Number of seconds to sleep for
	 */
	public static void sleepSeconds(double secs) {
		long millis = (long) (secs * MS_IN_SEC);
		sleepMillis(millis);
	}

	/**
	 * Sleep for the specific number of milliseconds
	 * @param millis Number of milliseconds to sleep for
	 */
	public static void sleepMillis(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
		}
	}

	/**
	 * Sleep for the specific number of microseconds
	 * @param micros Number of microseconds to sleep for
	 */
	public static void sleepMicros(int micros) {
		sleepNanos(0, TimeUnit.MICROSECONDS.toNanos(micros));
	}

	/**
	 * Sleep for the specified number of nanoseconds.
	 * @param nanos Number of nanoseconds
	 */
	public static void sleepNanos(int nanos) {
		sleepNanos(0, nanos);
	}
	
	public static void pause() {
		Object lock = new Object();
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	public static void busySleep(long nanos) {
		final long startTime = System.nanoTime();
		do {
		} while ((System.nanoTime() - startTime) < nanos);
	}
	
	public static native long sleepNanos(int seconds, long nanos);
	
	public static void main(String[] args) {
		LibraryLoader.loadLibrary(SleepUtil.class, "diozero-system-utils");
		System.out.println("Sleeping for 2.5s");
		sleepNanos(2, 500_000_000);
		System.out.println("Done");
	}
}
