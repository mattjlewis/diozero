package com.diozero.util;

public class SleepUtil {
	/**
	 *
	 * @param secs
	 */
	public static void sleepSeconds(double secs) {
		try {
			Thread.sleep((int) (secs * 1000));
		} catch (InterruptedException ex) {
		}
	}

	/**
	 *
	 * @param mili
	 */
	public static void sleepMillis(long mili) {
		try {
			Thread.sleep(mili);
		} catch (InterruptedException ex) {
		}
	}

	/**
	 *
	 * @param mili
	 * @param nano
	 */
	public static void sleep(int mili, int nano) {
		try {
			Thread.sleep(mili, nano);
		} catch (InterruptedException ex) {
		}
	}
}
