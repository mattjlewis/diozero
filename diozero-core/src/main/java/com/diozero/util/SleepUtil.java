package com.diozero.util;

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
	/**
	 * Sleep for the specific number of seconds
	 * @param secs number of seconds to sleep for
	 */
	public static void sleepSeconds(double secs) {
		try {
			Thread.sleep((int) (secs * 1000));
		} catch (InterruptedException ex) {
		}
	}

	/**
	 * Sleep for the specific number of milli-seconds
	 * @param mili number of millis seconds to sleep for
	 */
	public static void sleepMillis(long mili) {
		try {
			Thread.sleep(mili);
		} catch (InterruptedException ex) {
		}
	}

	/**
	 * Sleep for mill.nan seconds
	 * @param mili number of milli-seconds
	 * @param nano number of nano-seconds
	 */
	public static void sleep(int mili, int nano) {
		try {
			Thread.sleep(mili, nano);
		} catch (InterruptedException ex) {
		}
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
}
