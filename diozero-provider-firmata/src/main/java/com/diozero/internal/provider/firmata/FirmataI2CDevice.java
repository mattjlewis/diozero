package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Device I/O Zero - Firmata
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;
import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

/**
 * <p>Work In Progress. I am unclear as to how the this Java Firmata I2C implementation is supposed to work.</p>
 * <p>Wiring:</p>
 * <ul>
 * <li>SDA: A4</li>
 * <li>SCL: A5</li>
 * </ul>
 */
public class FirmataI2CDevice extends AbstractDevice implements I2CDeviceInterface, I2CListener {
	private static final int NO_REGISTER = 0;
	
	private I2CDevice i2cDevice;
	private Lock lock;
	private Map<Integer, Condition> conditions;
	private Map<Integer, LinkedList<I2CEvent>> eventQueues;

	public FirmataI2CDevice(FirmataDeviceFactory deviceFactory, String key, int controller, int address,
			int addressSize, int clockFrequency) {
		super(key, deviceFactory);
		
		lock = new ReentrantLock();
		conditions = new HashMap<>();
		eventQueues = new HashMap<>();
		
		Logger.info("Creating new Firmata I2CDevice for address 0x{}", Integer.toHexString(address));
		try {
			i2cDevice = deviceFactory.getIoDevice().getI2CDevice((byte) address);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	private void waitForData(int register, ByteBuffer buffer) {
		Logger.info("Waiting for data for register 0x{}", Integer.toHexString(register));
		lock.lock();
		Integer reg = Integer.valueOf(register);
		try {
			// Has the data already arrived?
			LinkedList<I2CEvent> event_queue_before = eventQueues.get(reg);
			if (event_queue_before != null && event_queue_before.size() > 0) {
				I2CEvent event = event_queue_before.remove();
				buffer.put(event.getData());
				return;
			}
			
			// Locate the condition for this register
			Condition condition = conditions.get(reg);
			if (condition == null) {
				condition = lock.newCondition();
				conditions.put(reg, condition);
			}
			Logger.info("calling await()");
			condition.await();
			Logger.info("returned from await()");
			
			LinkedList<I2CEvent> event_queue = eventQueues.get(reg);
			if (event_queue == null || event_queue.isEmpty()) {
				Logger.warn("No data available for register {}", reg);
			} else {
				I2CEvent event = event_queue.remove();
				buffer.put(event.getData());
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		Logger.info("read(0x{})", Integer.toHexString(register));
		try {
			i2cDevice.ask(register, (byte) buffer.remaining(), this);
			waitForData(register, buffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void read(ByteBuffer buffer) throws RuntimeException {
		try {
			i2cDevice.ask((byte) buffer.remaining(), this);
			waitForData(NO_REGISTER, buffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		byte[] data = new byte[buffer.remaining()+1];
		data[0] = (byte) register;
		buffer.get(data, 1, buffer.remaining());
		try {
			i2cDevice.tell(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void write(ByteBuffer buffer) throws RuntimeException {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		try {
			i2cDevice.tell(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		try { i2cDevice.stopReceivingUpdates(); } catch (IOException e) { }
		i2cDevice.unsubscribe(this);
	}

	@Override
	public void onReceive(I2CEvent event) {
		Logger.info(event);
		Integer register = Integer.valueOf(event.getRegister());
		lock.lock();
		try {
			Condition condition = conditions.get(register);
			if (condition == null) {
				Logger.warn("Got an I2C event for a register ({}) not being monitored", register);
			} else {
				LinkedList<I2CEvent> event_queue = eventQueues.get(register);
				if (event_queue == null) {
					event_queue = new LinkedList<>();
					eventQueues.put(register, event_queue);
				}
				event_queue.addLast(event);
				condition.signalAll();
			}
		} finally {
			lock.unlock();
		}
		event.getRegister();
	}
}
