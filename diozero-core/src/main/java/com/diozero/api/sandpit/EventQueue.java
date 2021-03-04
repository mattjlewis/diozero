package com.diozero.api.sandpit;

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
		future = DiozeroScheduler.getDaemonInstance().submit(this);
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
		running.set(false);
		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}
		future.cancel(true);
	}
	
	@Override
	public void run() {
		running.set(true);
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
