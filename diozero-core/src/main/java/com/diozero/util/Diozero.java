package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     Diozero.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.sbc.DeviceFactoryHelper;

public class Diozero {
	public static final int UNKNOWN_VALUE = -1;

	private static AtomicBoolean initialised = new AtomicBoolean(false);
	private static AtomicBoolean shutdown = new AtomicBoolean(false);
	private static List<AutoCloseable> closeables = new ArrayList<>();
	private static CountDownLatch doneSignal = new CountDownLatch(1);

	/**
	 * Initialise the diozero shutdown handler if not already initialised. Called
	 * from DeviceFactoryHelper.initialise(). Application code should not invoke
	 * this method.
	 */
	public static synchronized void initialiseShutdownHook() {
		if (initialised.get()) {
			return;
		}

		Runtime.getRuntime().addShutdownHook(new ShutdownHandlerThread());

		initialised.set(true);
	}

	/**
	 * Register an object to be explicitly closed in the case of abnormal shutdown
	 *
	 * @param closeableArray Array of closeable objects to close on shutdown
	 */
	public static void registerForShutdown(AutoCloseable... closeableArray) {
		for (AutoCloseable closeable : closeableArray) {
			closeables.add(closeable);
		}
	}

	/**
	 * Shutdown diozero.
	 */
	public static synchronized void shutdown() {
		Logger.trace("shutdown - START");

		if (shutdown.getAndSet(true)) {
			Logger.trace("Already shutdown");
			return;
		}

		// Stop all scheduled jobs
		DiozeroScheduler.shutdownAll();

		// First close all instances that have registered themselves with the
		// DeviceFactoryHelper
		if (closeables != null) {
			closeables.forEach(closeable -> {
				try {
					closeable.close();
				} catch (Exception e) {
					// Ignore
				}
			});
			closeables.clear();
		}

		// Then close the base native device factory which will close all
		// InternalDeviceInterface instances that are still open
		DeviceFactoryHelper.close();

		// Notify any threads waiting on diozero to shutdown
		doneSignal.countDown();

		Logger.trace("shutdown - END");
	}

	public static void waitForShutdown() {
		try {
			doneSignal.await();
		} catch (InterruptedException e) {
			// Ignore
			Logger.debug(e, "Interrupted: {}", e);
		}
	}

	private static class ShutdownHandlerThread extends Thread {
		public ShutdownHandlerThread() {
			setName("diozero Shutdown Handler");
			setDaemon(false);
		}

		@Override
		public void run() {
			Logger.debug("Shutdown handler running");
			shutdown();
			Logger.debug("Shutdown handler finished");
		}
	}
}
