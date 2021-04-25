package com.diozero.api.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     EventQueue.java
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.diozero.api.Event;
import com.diozero.util.DiozeroScheduler;

/**
 * Decouple event producers from consumers so that event production isn't affected by event consumption
 *
 * @param <T> the class containing the data fields for individual events
 */
public class EventQueue<T extends Event> implements Consumer<T>, Runnable {
	private Queue<T> queue;
	private List<Consumer<T>> listeners;
	private Lock lock;
	private Condition condition;
	private AtomicBoolean running;
	private Future<?> future;

	public EventQueue() {
		queue = new ConcurrentLinkedQueue<>();
		listeners = new ArrayList<>();
		lock = new ReentrantLock();
		condition = lock.newCondition();
		running = new AtomicBoolean();
		running.set(true);
		future = DiozeroScheduler.getNonDaemonInstance().submit(this);
	}
	
	public void addListener(Consumer<T> listener) {
		listeners.add(listener);
	}

	/**
	 * Adds an element to the tail of the queue
	 */
	@Override
	public void accept(T t) {
		queue.add(t);
		
		// Notify any listeners
		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}
	}
	
	public void stop() {
		if (running.get()) {
			running.set(false);
			lock.lock();
			try {
				condition.signal();
			} finally {
				lock.unlock();
			}
			future.cancel(true);
		}
	}
	
	@Override
	public void run() {
		while (running.get()) {
			lock.lock();
			try {
				condition.await();
				
				while (true) {
					T event = queue.poll();
					if (event == null) {
						break;
					}
					listeners.forEach(listener -> listener.accept(event));
				}
			} catch (InterruptedException e) {
				// Ignore
			} finally {
				lock.unlock();
			}
		}
	}
}
