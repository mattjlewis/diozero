package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     ShutdownTest.java
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

import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.util.Diozero;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

public class ShutdownTest implements AutoCloseable {
	public static void main(String[] args) {
		// Required to initialise tinylog
		Logger.debug("ShutdownTest");

		// Not normally required as this is invoked from DeviceFactoryHelper
		Diozero.initialiseShutdownHook();

		Object lock = new Object();

		try (ShutdownTest test = new ShutdownTest()) {
			Diozero.registerForShutdown(test);

			/*-
			Future<?> future = DiozeroScheduler.getDefaultInstance().scheduleAtFixedRate(
					() -> System.out.println("Run " + System.currentTimeMillis()), 1_000, 1_000, TimeUnit.MILLISECONDS);
			*/
			Future<?> future = DiozeroScheduler.getDefaultInstance().submit(() -> {
				try {
					while (!Thread.interrupted()) {
						long now_ms = System.currentTimeMillis();
						System.out.println("Run " + now_ms);
						Thread.sleep(0, 1);
						SleepUtil.busySleep(100_000_000);
					}
				} catch (InterruptedException e) {
					System.out.println("Interrupted in loop: " + e);
					e.printStackTrace(System.out);
					Thread.currentThread().interrupt();
				}
				System.out.println("Inner loop DONE");
			});

			System.out.println("Waiting...");

			// Diozero.waitForShutdown();
			// System.out.println("!!!! After waitfForShutdown...");

			future.get();
			System.out.println("!!!! After future.get()");

			/*-
			synchronized (lock) {
				lock.wait();
			}
			System.out.println("!!!! After lock.wait()");
			*/
		} catch (Throwable t) {
			System.out.println("Error in main: " + t);
			t.printStackTrace(System.out);
		} finally {
			System.out.println("Calling Diozero.shutdown()...");
			Diozero.shutdown();
		}
		System.out.println("Done.");
	}

	@Override
	public void close() {
		System.out.println("close() - START");
		try {
			System.out.println("close - sleeping");
			Thread.sleep(100);
		} catch (InterruptedException e) {
			System.out.println("Error: " + e);
		}
		System.out.println("close() - DONE");
	}
}
