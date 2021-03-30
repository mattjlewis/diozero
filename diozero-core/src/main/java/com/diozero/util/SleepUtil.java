package com.diozero.util;

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
