/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SchedulerTest.java
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
package com.diozero.util;

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

public class SchedulerTest {
	static int scheduler_instance = 0;
	static long last_call = 0;

	public static void main(String[] args) {
		test1();
		test2();
		test3();
		test4();
	}

	private static void test1() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0, new TestThreadFactory(false));

		ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			long diff = now - last_call;
			last_call = now;
			System.out.format("Thread 1: %s, Time between calls: %d%n", Thread.currentThread().getName(),
					Long.valueOf(diff));
		}, 100, 100, TimeUnit.MILLISECONDS);
		ScheduledFuture<?> future2 = scheduler.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			long diff = now - last_call;
			last_call = now;
			System.out.format("Thread 2: %s, Time between calls: %d%n", Thread.currentThread().getName(),
					Long.valueOf(diff));
		}, 100, 100, TimeUnit.MILLISECONDS);
		ScheduledFuture<?> future3 = scheduler.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			long diff = now - last_call;
			last_call = now;
			System.out.format("Thread 3: %s, Time between calls: %d%n", Thread.currentThread().getName(),
					Long.valueOf(diff));
		}, 100, 100, TimeUnit.MILLISECONDS);

		SleepUtil.sleepSeconds(2);

		future1.cancel(true);
		future2.cancel(true);
		future3.cancel(true);
		try {
			future1.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}
		try {
			future2.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}
		try {
			future3.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}

		scheduler.shutdownNow();

		System.out.println("test1 finished");
	}

	private static void test2() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);

		Future<?> future1 = scheduler.submit(() -> {
			while (true) {
				SleepUtil.sleepMillis(100);
				System.out.println("Thread 1 running");
			}
		});
		Future<?> future2 = scheduler.submit(() -> {
			while (true) {
				SleepUtil.sleepMillis(100);
				System.out.println("Thread 2 running");
			}
		});
		Future<?> future3 = scheduler.submit(() -> {
			while (true) {
				SleepUtil.sleepMillis(100);
				System.out.println("Thread 3 running");
			}
		});

		SleepUtil.sleepSeconds(2);

		future1.cancel(true);
		try {
			future1.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}

		SleepUtil.sleepSeconds(1);

		future2.cancel(true);
		try {
			future2.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}

		SleepUtil.sleepSeconds(1);

		future3.cancel(true);
		try {
			future3.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}

		scheduler.shutdownNow();

		System.out.println("test2 finished");
	}

	private static void test3() {
		ExecutorService executor = Executors.newScheduledThreadPool(0);

		executor.execute(() -> {
			try {
				while (true) {
					Thread.sleep(100);
					System.out.println("Thread 1 running");
				}
			} catch (InterruptedException e) {
				// Ignore
				System.out.println("Interrupted");
			}
		});
		executor.execute(() -> {
			try {
				while (true) {
					Thread.sleep(100);
					System.out.println("Thread 2 running");
				}
			} catch (InterruptedException e) {
				// Ignore
				System.out.println("Interrupted");
			}
		});
		executor.execute(() -> {
			try {
				while (true) {
					Thread.sleep(100);
					System.out.println("Thread 3 running");
				}
			} catch (InterruptedException e) {
				// Ignore
				System.out.println("Interrupted");
			}
		});

		SleepUtil.sleepSeconds(2);

		executor.shutdownNow();

		System.out.println("test3 finished");
	}

	private static void test4() {
		ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());

		Future<?> future1 = executor.submit(() -> {
			while (true) {
				SleepUtil.sleepMillis(100);
				System.out.println("Thread 1 running");
			}
		});
		Future<?> future2 = executor.submit(() -> {
			while (true) {
				SleepUtil.sleepMillis(100);
				System.out.println("Thread 2 running");
			}
		});
		Future<?> future3 = executor.submit(() -> {
			while (true) {
				SleepUtil.sleepMillis(100);
				System.out.println("Thread 3 running");
			}
		});

		SleepUtil.sleepSeconds(2);

		future1.cancel(true);
		future2.cancel(true);
		future3.cancel(true);
		try {
			future1.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}
		try {
			future2.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}
		try {
			future3.get();
		} catch (Exception e) {
			// Ignore
			System.out.println("Error: " + e);
		}

		executor.shutdownNow();

		System.out.println("test4 finished");
	}
}

class TestThreadFactory implements ThreadFactory {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final boolean daemon;

	TestThreadFactory(boolean daemon) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = "daemon-pool-" + poolNumber.getAndIncrement() + "-thread-";
		this.daemon = daemon;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		t.setDaemon(daemon);
		return t;
	}
}
