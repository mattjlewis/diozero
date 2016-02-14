package com.diozero.api;

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

public class GpioScheduler {
	private static GpioScheduler instance = new GpioScheduler();
	private ExecutorService daemonExecutor;
	private ScheduledExecutorService daemonScheduler;
	private ExecutorService nonDaemonExecutor;
	private ScheduledExecutorService nonDaemonScheduler;
	
	private GpioScheduler() {
		daemonExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory(true));
		daemonScheduler = Executors.newScheduledThreadPool(50, new DaemonThreadFactory(true));
		nonDaemonExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory(false));
		nonDaemonScheduler = Executors.newScheduledThreadPool(50, new DaemonThreadFactory(false));
	}
	
	public static GpioScheduler getInstance() {
		return instance;
	}
	
	public void execute(Runnable r) {
		execute(r, true);
	}
	
	public void execute(Runnable r, boolean daemon) {
		if (daemon) {
			daemonExecutor.execute(r);
		} else {
			nonDaemonExecutor.execute(r);
		}
	}
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long period, TimeUnit unit) {
		return scheduleAtFixedRate(r, initialDelay, period, unit, true);
	}
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long period, TimeUnit unit, boolean daemon) {
		if (daemon) {
			return daemonScheduler.scheduleAtFixedRate(r, initialDelay, period, unit);
		}
		return nonDaemonScheduler.scheduleAtFixedRate(r, initialDelay, period, unit);
	}
	
	public ScheduledFuture<?> invokeAtFixedRate(Supplier<Float> source, Consumer<Float> sink,
			long initialDelay, long period, TimeUnit unit) {
		return invokeAtFixedRate(source, sink, initialDelay, period, unit, true);
	}
	
	public ScheduledFuture<?> invokeAtFixedRate(Supplier<Float> source, Consumer<Float> sink,
			long initialDelay, long period, TimeUnit unit, boolean daemon) {
		if (daemon) {
			return daemonScheduler.scheduleAtFixedRate(() -> sink.accept(source.get()), initialDelay, period, unit);
		}
		return nonDaemonScheduler.scheduleAtFixedRate(() -> sink.accept(source.get()), initialDelay, period, unit);
	}
	
	public void shutdown() {
		try { daemonExecutor.awaitTermination(50, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { }
		try { daemonScheduler.awaitTermination(50, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { }
		try { nonDaemonExecutor.awaitTermination(50, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { }
		try { nonDaemonScheduler.awaitTermination(50, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { }
	}
	
	static class DaemonThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;
		private boolean daemon;

		DaemonThreadFactory(boolean daemon) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = (daemon ? "daemon" : "non-daemon") + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (daemon) {
				t.setDaemon(true);
			}
			return t;
		}
	}
}
