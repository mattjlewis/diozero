package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DiozeroScheduler.java
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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.tinylog.Logger;

import com.diozero.api.function.FloatConsumer;
import com.diozero.api.function.FloatSupplier;

public class DiozeroScheduler {
	private static DiozeroScheduler daemonInstance;
	private static DiozeroScheduler nonDaemonInstance;

	/**
	 * Get the default scheduler instance (non-daemon).
	 *
	 * @return the default scheduler
	 */
	public static DiozeroScheduler getDefaultInstance() {
		return getNonDaemonInstance();
	}

	/**
	 * Get the diozero scheduler instance that exclusively uses non-daemon threads
	 * (aka user threads).
	 *
	 * Non-daemon / user threads are high-priority threads (in the context of the
	 * JVM). The JVM will wait for any user thread to complete its task before
	 * terminating.
	 *
	 * @return the diozero scheduler instance that uses daemon threads
	 */
	public static synchronized DiozeroScheduler getNonDaemonInstance() {
		if (nonDaemonInstance == null || nonDaemonInstance.isShutdown()) {
			nonDaemonInstance = new DiozeroScheduler(false);
		}
		return nonDaemonInstance;
	}

	/**
	 * Get the diozero scheduler instance that exclusively uses daemon threads.
	 *
	 * A daemon thread is a low priority thread (in context of the JVM) whose only
	 * role is to provide services to user threads. A daemon thread does not prevent
	 * the JVM from exiting (even if the daemon thread itself is running).
	 *
	 * @return the diozero scheduler instance that uses daemon threads
	 */
	public static synchronized DiozeroScheduler getDaemonInstance() {
		if (daemonInstance == null || daemonInstance.isShutdown()) {
			daemonInstance = new DiozeroScheduler(true);
		}
		return daemonInstance;
	}

	public static void shutdownAll() {
		if (daemonInstance != null) {
			daemonInstance.shutdown();
		}
		if (nonDaemonInstance != null) {
			nonDaemonInstance.shutdown();
		}
	}

	private ScheduledExecutorService scheduler;
	private ExecutorService executor;
	private DaemonThreadFactory threadFactory;

	private DiozeroScheduler(boolean daemon) {
		threadFactory = new DaemonThreadFactory(daemon);
		scheduler = Executors.newScheduledThreadPool(0, threadFactory);
		// Note pool size is 0 and keepAliveTime is 0 to prevent shutdown delays
		executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}

	public void execute(Runnable command) {
		executor.execute(command);
	}

	public Future<?> submit(Runnable task) {
		return executor.submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return executor.submit(task, result);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	public ScheduledFuture<?> invokeAtFixedRate(FloatSupplier source, FloatConsumer sink, long initialDelay,
			long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(() -> sink.accept(source.getAsFloat()), initialDelay, period, unit);
	}

	private void shutdown() {
		scheduler.shutdownNow();
		executor.shutdownNow();
		Logger.trace("Shutdown - done");
	}

	public boolean isShutdown() {
		return scheduler.isShutdown() && executor.isShutdown();
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
