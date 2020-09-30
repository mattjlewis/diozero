package com.diozero.internal.provider.test;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     HCSR04EchoPin.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class HCSR04EchoPin extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, Runnable {
	private static final Random random = new Random();
	private static HCSR04EchoPin instance;
	
	static HCSR04EchoPin getInstance() {
		return instance;
	}
	
	private int gpio;
	private Lock lock;
	private Condition cond;
	private AtomicBoolean value;
	private long echoStart;
	private AtomicBoolean running;
	private ExecutorService executor;
	
	public HCSR04EchoPin(String key, DeviceFactoryInterface deviceFactory,
			int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.gpio = gpio;
		
		instance = this;
		
		lock = new ReentrantLock();
		cond = lock.newCondition();
		value = new AtomicBoolean();
		running = new AtomicBoolean();
		
		// Start the background process to send the echo low signal
		executor = Executors.newFixedThreadPool(1);
		executor.execute(this);
	}
	
	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return value.get();
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		
		running.getAndSet(false);
		lock.lock();
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
		executor.shutdownNow();
	}
	
	void doEcho(long start) {
		try {
			Logger.debug("doEcho()");
			
			if (System.currentTimeMillis() - start < 50) {
				Thread.sleep(random.nextInt(50));
			}
			echoStart = System.currentTimeMillis();
			value.getAndSet(true);
			valueChanged(new DigitalInputEvent(gpio, echoStart, System.nanoTime(), true));
			// Need to send echo low in a separate thread
			lock.lock();
			try {
				cond.signalAll();
			} finally {
				lock.unlock();
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		}
	}

	@Override
	public void run() {
		running.getAndSet(true);
		try {
			while (running.get()) {
				lock.lock();
				try {
					cond.await();
				} finally {
					lock.unlock();
				}
				if (!running.get()) {
					break;
				}
				if ((System.currentTimeMillis() - echoStart) < 2) {
					Thread.sleep(0, random.nextInt(999_999));
				}
				value.getAndSet(false);
				valueChanged(new DigitalInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), false));
				Logger.debug("Time to send echo high then low=" + (System.currentTimeMillis() - echoStart) + "ms");
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		}
	}
}
