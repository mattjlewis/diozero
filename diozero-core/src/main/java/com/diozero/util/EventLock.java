package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     EventLock.java  
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


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EventLock {
	private Lock lock = new ReentrantLock();
	private Condition cond = lock.newCondition();
	private boolean wasSet;

	/**
	 * Wait indefinitely for set() to be called.
	 * @return True if set() was called, false woken unexpectedly.
	 * @throws InterruptedException If interrupted.
	 */
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

	/**
	 * Wait the specified time period for set() to be called.
	 * @param timeout Timeout value in milliseconds.
	 * @return True if set() was called, false if timed out waiting.
	 * @throws InterruptedException If interrupted.
	 */
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
