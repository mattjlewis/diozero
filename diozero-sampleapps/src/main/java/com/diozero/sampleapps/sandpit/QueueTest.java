package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     QueueTest.java
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QueueTest {
	private static final int MAX_EVENTS = 1000;
	
	private static Queue<Integer> queue;
	private static Lock lock;
	private static Condition mainCondition;
	private static Condition condition;
	
	public static void main(String[] args) {
		queue = new ConcurrentLinkedQueue<>();
		//queue = new ConcurrentLinkedDeque<>();
		lock = new ReentrantLock();
		mainCondition = lock.newCondition();
		condition = lock.newCondition();
		
		ExecutorService es = Executors.newFixedThreadPool(2);
		es.execute(QueueTest::processEvents);
		es.execute(QueueTest::addEvents);
		
		lock.lock();
		try {
			System.out.println("Waiting for events to be processed");
			mainCondition.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		
		System.out.println("Shutting down executor service");
		es.shutdown();
		
		System.out.println("Finished");
	}
	
	private static void addEvents() {
		System.out.println("Adding events");
		for (int i=0; i<MAX_EVENTS; i++) {
			queue.add(Integer.valueOf(i));
			
			lock.lock();
			try {
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
		System.out.println("Finished adding events");
	}
	
	private static void processEvents() {
		System.out.println("Processing events");
		boolean finished = false;
		Integer prev_value = null;
		while (! finished) {
			lock.lock();
			try {
				System.out.println("Waiting on condition");
				condition.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
			System.out.println("queue size: " + queue.size());
			do {
				Integer i = queue.poll();
				if (i == null) {
					break;
				}
				
				finished = i.intValue() == MAX_EVENTS-1;
				
				if (prev_value != null) {
					if (prev_value.intValue() != i.intValue() - 1) {
						System.out.println("Error, expected " + (prev_value.intValue() + 1));
					}
				}
				
				prev_value = i;
			} while (! queue.isEmpty());
		}
		
		lock.lock();
		try {
			System.out.println("Notifying the main condition");
			mainCondition.signal();
		} finally {
			lock.unlock();
		}
	}
}
