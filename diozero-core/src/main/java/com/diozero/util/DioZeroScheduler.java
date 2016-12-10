package com.diozero.util;

import java.util.Map;

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


import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.pmw.tinylog.Logger;

public class DioZeroScheduler {
	private static DioZeroScheduler daemonInstance = new DioZeroScheduler(true);
	private static DioZeroScheduler nonDaemonInstance = new DioZeroScheduler(false);
	
	private ExecutorService executor;
	private ScheduledExecutorService scheduler;
	private DaemonThreadFactory threadFactory;
	
	public static DioZeroScheduler getDaemonInstance() {
		return daemonInstance;
	}
	
	public static DioZeroScheduler getNonDaemonInstance() {
		return nonDaemonInstance;
	}
	
	public static void shutdownAll() {
		daemonInstance.shutdown();
		nonDaemonInstance.shutdown();
	}
	
	private DioZeroScheduler(boolean daemon) {
		threadFactory = new DaemonThreadFactory(daemon);
		executor = Executors.newCachedThreadPool(threadFactory);
		scheduler = Executors.newScheduledThreadPool(50, threadFactory);
	}
	
	public void execute(Runnable r) {
		executor.execute(r);
	}
	
	public Future<?> submit(Runnable r) {
		return executor.submit(r);
	}
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(r, initialDelay, period, unit);
	}
	
	public ScheduledFuture<?> invokeAtFixedRate(Supplier<Float> source, Consumer<Float> sink,
			long initialDelay, long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(() -> sink.accept(source.get()), initialDelay, period, unit);
	}
	
	private void shutdown() {
		executor.shutdown();
		scheduler.shutdown();
		Logger.debug("Shutdown - done");
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
			Logger.debug("[" + (daemon ? "daemon" : "non-daemon") + "] activeCount=" + group.activeCount() +
					", activeGroupCount=" + group.activeGroupCount());
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
