package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataI2CDevice.java
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

import com.diozero.api.I2CConstants;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter.I2CResponse;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalI2CDeviceInterface;

/**
 * <p>
 * Work In Progress. I am unclear as to how the this Java Firmata I2C
 * implementation is supposed to work.
 * </p>
 * <p>
 * Wiring:
 * </p>
 * <ul>
 * <li>SDA: A4</li>
 * <li>SCL: A5</li>
 * </ul>
 */
public class FirmataI2CDevice extends AbstractDevice implements InternalI2CDeviceInterface, I2CListener {
	private static final int NO_REGISTER = 0;

	private FirmataAdapter adapter;
	private int address;
	private boolean autoRestart = false;
	private boolean addressSize10Bit;

	// Firmata4j specific
	private I2CDevice i2cDevice;
	private Lock lock;
	private Map<Integer, Condition> conditions;
	private Map<Integer, LinkedList<I2CEvent>> eventQueues;

	public FirmataI2CDevice(FirmataDeviceFactory deviceFactory, String key, int controller, int address,
			I2CConstants.AddressSize addressSize) {
		super(key, deviceFactory);

		adapter = deviceFactory.getFirmataAdapter();
		this.address = address;
		addressSize10Bit = addressSize == I2CConstants.AddressSize.SIZE_10;

		Logger.trace("Creating new Firmata I2CDevice for address 0x{}", Integer.toHexString(address));
		if (adapter == null) {
			lock = new ReentrantLock();
			conditions = new HashMap<>();
			eventQueues = new HashMap<>();

			try {
				i2cDevice = deviceFactory.getIoDevice().getI2CDevice((byte) address);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
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
		if (adapter != null) {
			I2CResponse response = adapter.i2cRead(address, autoRestart, addressSize10Bit, 1);
			data = response.getData()[0];
		} else {
			try {
				i2cDevice.ask(NO_REGISTER, (byte) 1, this);
				byte[] buffer = waitForData(NO_REGISTER);
				data = buffer[0];
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}

		return data;
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		if (adapter != null) {
			adapter.i2cWrite(address, autoRestart, addressSize10Bit, new byte[] { b });
		} else {
			try {
				i2cDevice.tell(b);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	@Override
	public byte readByteData(int register) {
		byte data;
		if (adapter != null) {
			I2CResponse response = adapter.i2cReadData(address, autoRestart, addressSize10Bit, register, 1);
			data = response.getData()[0];
		} else {
			try {
				i2cDevice.ask(register, (byte) 1, this);
				byte[] buffer = waitForData(register);
				data = buffer[0];
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}

		return data;
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		if (adapter != null) {
			adapter.i2cWriteData(address, autoRestart, addressSize10Bit, register, new byte[] { b });
		} else {
			byte[] data = new byte[2];
			data[0] = (byte) register;
			data[1] = b;
			try {
				i2cDevice.tell(data);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		short s;
		if (adapter != null) {
			I2CResponse response = adapter.i2cReadData(address, autoRestart, addressSize10Bit, register, 2);
			// SMBus assumes little endian
			s = (short) (response.getData()[0] & 0xff | (response.getData()[1] << 8));
		} else {
			try {
				i2cDevice.ask(register, (byte) 2, this);
				ByteBuffer bb = ByteBuffer.wrap(waitForData(register));
				// SMBus assumes little endian
				bb.order(ByteOrder.LITTLE_ENDIAN);
				s = bb.getShort();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		return s;
	}

	@Override
	public void writeWordData(int register, short data) throws RuntimeIOException {
		if (adapter != null) {
			byte[] tx_data = new byte[3];
			// Note SMBus assumes little endian (LSB first)
			tx_data[0] = (byte) (data & 0xff);
			tx_data[1] = (byte) ((data >> 8) & 0xff);
			adapter.i2cWriteData(address, autoRestart, addressSize10Bit, register, tx_data);
		} else {
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
	}

	@Override
	public short processCall(int register, short data) throws RuntimeIOException {
		writeWordData(register, data);
		return readWordData(register);
	}

	@Override
	public byte[] readBlockData(int register) throws RuntimeIOException {
		byte[] buffer = new byte[MAX_I2C_BLOCK_SIZE];
		int read = readI2CBlockData(register, buffer);
		/*-
		 * This command reads a block of up to 32 bytes from a device, from a designated
		 * register that is specified through the Comm byte. The amount of data is
		 * specified by the device in the Count byte.
		 *
		 * S Addr Wr [A] Comm [A]
		 * 		S Addr Rd [A] [Count] A [Data] A [Data] A ... A [Data] NA P
		 * As opposed to:
		 * S Addr Wr [A] Comm [A]
		 *      S Addr Rd [A] [Data] A [Data] A ... A [Data] NA P
		 */
		// The first byte read is the COUNT of bytes read
		int count = buffer[0] & 0xff;
		if (count > read) {
			throw new RuntimeIOException("Error Count byte indicated that there were more bytes to read (" + count
					+ ") than actually read (" + read + ")");
		}
		byte[] rx_data = new byte[count];
		System.arraycopy(buffer, 1, rx_data, 0, count);
		return rx_data;
	}

	@Override
	public void writeBlockData(int register, byte... data) throws RuntimeIOException {
		// First byte is the Count of bytes to write
		byte[] tx_data = new byte[data.length + 1];
		tx_data[0] = (byte) data.length;
		System.arraycopy(data, 0, tx_data, 1, data.length);
		writeI2CBlockData(register, data);
	}

	@Override
	public byte[] blockProcessCall(int register, byte... data) throws RuntimeIOException {
		writeI2CBlockData(register, data);
		byte[] rx_data = new byte[data.length];
		readI2CBlockData(register, rx_data);
		return rx_data;
	}

	@Override
	public int readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		byte[] rx_data;
		if (adapter != null) {
			I2CResponse response = adapter.i2cReadData(address, autoRestart, addressSize10Bit, register, buffer.length);
			rx_data = response.getData();
		} else {
			try {
				i2cDevice.ask(register, (byte) buffer.length, this);
				rx_data = waitForData(register);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		System.arraycopy(rx_data, 0, buffer, 0, rx_data.length);
		return rx_data.length;
	}

	@Override
	public void writeI2CBlockData(int register, byte... data) throws RuntimeIOException {
		if (adapter != null) {
			adapter.i2cWriteData(address, autoRestart, addressSize10Bit, register, data);
		} else {
			byte[] tx_data = new byte[data.length + 1];
			tx_data[0] = (byte) register;
			System.arraycopy(data, 0, tx_data, 1, data.length);
			try {
				i2cDevice.tell(tx_data);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		byte[] rx_data;

		if (adapter != null) {
			I2CResponse response = adapter.i2cRead(address, autoRestart, addressSize10Bit, buffer.length);
			rx_data = response.getData();
		} else {
			try {
				i2cDevice.ask((byte) buffer.length, this);
				rx_data = waitForData(NO_REGISTER);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}

		System.arraycopy(rx_data, 0, buffer, 0, rx_data.length);
		return rx_data.length;
	}

	@Override
	public void writeBytes(byte... data) throws RuntimeIOException {
		if (adapter != null) {
			adapter.i2cWrite(address, autoRestart, addressSize10Bit, data);
		} else {
			try {
				i2cDevice.tell(data);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	@Override
	public void readWrite(I2CMessage[] messages, byte[] buffer) {
		throw new UnsupportedOperationException("I2C readWrite not supported");
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		if (adapter == null) {
			try {
				i2cDevice.stopReceivingUpdates();
			} catch (IOException e) {
			}
			i2cDevice.unsubscribe(this);
		}
	}

	@Override
	// Firmata4j specific
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

	// FIXME Change from ByteBuffer to byte array?
	// Firmata4j specific
	private byte[] waitForData(int register) {
		Logger.info("Waiting for data for register 0x{}", Integer.toHexString(register));

		byte[] rx_data = null;

		lock.lock();
		Integer reg = Integer.valueOf(register);
		try {
			// Has the data already arrived?
			LinkedList<I2CEvent> event_queue_before = eventQueues.get(reg);
			if (event_queue_before != null && event_queue_before.size() > 0) {
				I2CEvent event = event_queue_before.remove();
				rx_data = event.getData();
			} else {
				// Locate the condition for this register
				Condition condition = conditions.get(reg);
				if (condition == null) {
					condition = lock.newCondition();
					conditions.put(reg, condition);
				}
				Logger.debug("calling await()");
				condition.await();
				Logger.debug("returned from await()");

				LinkedList<I2CEvent> event_queue = eventQueues.get(reg);
				if (event_queue == null || event_queue.isEmpty()) {
					Logger.warn("No data available for register {}", reg);
				} else {
					I2CEvent event = event_queue.remove();
					rx_data = event.getData();
				}
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		} finally {
			lock.unlock();
		}

		return rx_data;
	}
}
