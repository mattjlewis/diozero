package com.diozero.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Event {
	private Lock lock = new ReentrantLock();
	private Condition cond = lock.newCondition();
	private boolean wasSet;

	public boolean doWait() throws InterruptedException {
		lock.lock();
		wasSet = false;
		try {
			cond.await();
		} finally {
			lock.unlock();
		}
		return wasSet;
	}

	public boolean doWait(int timeout) throws InterruptedException {
		lock.lock();
		wasSet = false;
		try {
			cond.await(timeout, TimeUnit.MILLISECONDS);
		} finally {
			lock.unlock();
		}
		return wasSet;
	}

	public void set() {
		lock.lock();
		wasSet = true;
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void clear() {
		lock.lock();
		wasSet = false;
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}
}
