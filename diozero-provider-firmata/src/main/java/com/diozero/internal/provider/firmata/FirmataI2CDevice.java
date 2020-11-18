package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Firmata
 * Filename:     FirmataI2CDevice.java  
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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;
import org.tinylog.Logger;

import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.I2CDeviceInterface;
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
			int addressSize) {
		super(key, deviceFactory);
		
		lock = new ReentrantLock();
		conditions = new HashMap<>();
		eventQueues = new HashMap<>();
		
		Logger.trace("Creating new Firmata I2CDevice for address 0x{}", Integer.toHexString(address));
		try {
			i2cDevice = deviceFactory.getIoDevice().getI2CDevice((byte) address);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	// FIXME Change from ByteBuffer to byte array? 
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
				// TODO Check that the length of data is what was requested
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
				// TODO Check that the length of data is what was requested
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean probe(com.diozero.api.I2CDevice.ProbeMode mode) {
		return readByte() >= 0;
	}
	
	@Override
	public void writeQuick(byte bit) {
		throw new UnsupportedOperationException("writeQuick operation is unsupported");
	}

	@Override
	public byte readByte() {
		Logger.debug("read()");
		byte data;
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(1);
			i2cDevice.ask(NO_REGISTER, (byte) 1, this);
			waitForData(NO_REGISTER, buffer);
			data = buffer.get(0);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
		return data;
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		try {
			i2cDevice.tell(b);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public byte readByteData(int register) {
		byte data;
		try {
			ByteBuffer buffer = ByteBuffer.allocateDirect(1);
			i2cDevice.ask(register, (byte) 1, this);
			waitForData(register, buffer);
			data = buffer.get(0);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
		return data;
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		byte[] data = new byte[2];
		data[0] = (byte) register;
		data[1] = b;
		try {
			i2cDevice.tell(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		short s;
		try {
			i2cDevice.ask(register, (byte) 2, this);
			ByteBuffer bb = ByteBuffer.allocateDirect(2);
			// SMBus assumes little endian
			bb.order(ByteOrder.LITTLE_ENDIAN);
			waitForData(register, bb);
			s = bb.getShort();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return s;
	}

	@Override
	public void writeWordData(int register, short data) throws RuntimeIOException {
		byte[] tx_data = new byte[3];
		tx_data[0] = (byte) register;
		// Note SMBus assumes little endian (LSB first)
		tx_data[1] = (byte) (data & 0xff);
		tx_data[2] = (byte) ((data >> 8) & 0xff);
		try {
			i2cDevice.tell(tx_data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		try {
			i2cDevice.ask((byte) buffer.length, this);
			ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length);
			waitForData(NO_REGISTER, bb);
			bb.get(buffer);
			return buffer.length;
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void writeBytes(byte[] data) throws RuntimeIOException {
		try {
			i2cDevice.tell(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public short processCall(int register, short data) throws RuntimeIOException {
		writeWordData(register, data);
		return readWordData(register);
	}

	@Override
	public int readBlockData(int register, byte[] buffer) throws RuntimeIOException {
		readI2CBlockData(register, buffer);
		return buffer.length;
	}

	@Override
	public void writeBlockData(int register, byte[] data) throws RuntimeIOException {
		writeI2CBlockData(register, data);
	}

	@Override
	public byte[] blockProcessCall(int register, byte[] data) throws RuntimeIOException {
		writeI2CBlockData(register, data);
		byte[] rx_data = new byte[data.length];
		readI2CBlockData(register, rx_data);
		return rx_data;
	}

	@Override
	public void readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		try {
			i2cDevice.ask(register, (byte) buffer.length, this);
			ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length);
			waitForData(register, bb);
			bb.get(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void writeI2CBlockData(int register, byte[] data) throws RuntimeIOException {
		byte[] tx_data = new byte[data.length+1];
		tx_data[0] = (byte) register;
		System.arraycopy(data, 0, tx_data, 1, data.length);
		try {
			i2cDevice.tell(tx_data);
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
		Logger.debug(event);
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
