package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     DiozeroScheduler.java
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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.tinylog.Logger;

public class DiozeroScheduler {
	private static DiozeroScheduler daemonInstance;
	private static DiozeroScheduler nonDaemonInstance;

	private ScheduledExecutorService scheduler;
	private DaemonThreadFactory threadFactory;

	public static synchronized DiozeroScheduler getDaemonInstance() {
		if (daemonInstance == null || daemonInstance.isShutdown()) {
			daemonInstance = new DiozeroScheduler(true);
		}
		return daemonInstance;
	}

	public static synchronized DiozeroScheduler getNonDaemonInstance() {
		if (nonDaemonInstance == null || nonDaemonInstance.isShutdown()) {
			nonDaemonInstance = new DiozeroScheduler(false);
		}
		return nonDaemonInstance;
	}

	public static void shutdownAll() {
		if (daemonInstance != null) {
			daemonInstance.shutdown();
		}
		if (nonDaemonInstance != null) {
			nonDaemonInstance.shutdown();
		}
	}

	private DiozeroScheduler(boolean daemon) {
		threadFactory = new DaemonThreadFactory(daemon);
		scheduler = Executors.newScheduledThreadPool(0, threadFactory);
	}

	public void execute(Runnable command) {
		scheduler.execute(command);
	}

	public Future<?> submit(Runnable task) {
		return scheduler.submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return scheduler.submit(task, result);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	public ScheduledFuture<?> invokeAtFixedRate(Supplier<Float> source, Consumer<Float> sink, long initialDelay,
			long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(() -> sink.accept(source.get()), initialDelay, period, unit);
	}

	private void shutdown() {
		scheduler.shutdownNow();
		Logger.trace("Shutdown - done");
	}

	public boolean isShutdown() {
		return scheduler.isShutdown();
	}

	static class DaemonThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);

		private final boolean daemon;
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		DaemonThreadFactory(boolean daemon) {
			this.daemon = daemon;

			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = (daemon ? "daemon" : "non-daemon") + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			t.setDaemon(daemon);
			return t;
		}

		void status() {
			Logger.debug("[" + (daemon ? "daemon" : "non-daemon") + "] activeCount=" + group.activeCount()
					+ ", activeGroupCount=" + group.activeGroupCount());
		}
	}

	public static void statusAll() {
		Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
		for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
			Logger.debug("Stack trace elements for Thread " + entry.getKey().getName() + ":");
			for (StackTraceElement element : entry.getValue()) {
				Logger.debug(element.toString());
			}
		}
		daemonInstance.status();
		nonDaemonInstance.status();
	}

	private void status() {
		threadFactory.status();
	}
}
