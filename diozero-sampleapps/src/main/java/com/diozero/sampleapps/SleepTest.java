package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     SleepTest.java  
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

import org.tinylog.Logger;

import com.diozero.util.SleepUtil;

public class SleepTest {
	public static void main(String[] args) throws InterruptedException {
		testMillisecondSleep();
		// testMicrosecondSleep();
		testNanosecondSleep();
	}

	private static void testMillisecondSleep() throws InterruptedException {
		Logger.info("Testing millisecond sleep accuracy...");

		int sleep_ms = 10;

		// First of all make sure the code is loaded into the JIT
		for (int i = 0; i < 10; i++) {
			SleepUtil.sleepMillis(sleep_ms);
			Thread.sleep(sleep_ms);
			SleepUtil.busySleep(sleep_ms * 1_000_000);
		}

		Logger.info("diozero sleep");
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			SleepUtil.sleepMillis(sleep_ms);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + (duration / 1_000_000f) + " ms, difference="
					+ (duration - sleep_ms * 1_000_000) / 1_000_000f + " ms");
		}

		Logger.info("Java sleep");
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			Thread.sleep(sleep_ms);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + (duration / 1_000_000f) + " ms, difference="
					+ (duration - sleep_ms * 1_000_000) / 1_000_000f + " ms");
		}
	}

	private static void testNanosecondSleep() throws InterruptedException {
		Logger.info("Testing nanosecond sleep accuracy...");

		int sleep_ns = 10_000;

		// First of all make sure the code is loaded into the JIT
		/*
		for (int i = 0; i < 10; i++) {
			SleepUtil.sleepNanos(sleep_ns);
			Thread.sleep(0, sleep_ns);
			SleepUtil.busySleep(sleep_ns);
		}

		Logger.info("JNI sleep");
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			SleepUtil.sleepNanos(sleep_ns);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + duration + " ns, difference=" + (duration - sleep_ns) + " ns");
		}
		*/

		Logger.info("Java sleep");
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			Thread.sleep(0, sleep_ns);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + duration + " ns, difference=" + (duration - sleep_ns) + " ns");
		}

		Logger.info("Busy sleep");
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			SleepUtil.busySleep(sleep_ns);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + duration + " ns, difference=" + (duration - sleep_ns) + " ns");
		}
	}
}
