package com.diozero.api;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GpioScheduler {
	private static GpioScheduler instance = new GpioScheduler();
	private ExecutorService executor;
	private ScheduledExecutorService scheduler;
	
	private GpioScheduler() {
		executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
		scheduler = Executors.newScheduledThreadPool(50, new DaemonThreadFactory());
	}
	
	public static GpioScheduler getInstance() {
		return instance;
	}
	
	public void execute(Runnable r) {
		executor.execute(r);
	}
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long period, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(r, initialDelay, period, unit);
	}
	
	public void shutdown() {
		try { executor.awaitTermination(50, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { }
		try { scheduler.awaitTermination(50, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { }
	}
	
	static class DaemonThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		DaemonThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "daemon-pool-" + poolNumber.getAndIncrement() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			t.setDaemon(true);
			return t;
		}
	}
}
