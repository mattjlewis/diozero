package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     I2CDevice.java  
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.internal.provider.I2CDeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.BitManipulation;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

/**
 * Utility class reading / writing to I2C devices.
 */
public class I2CDevice implements I2CConstants, I2CSMBusInterface {
	public static enum ProbeMode {
		QUICK, READ, AUTO;
	}

	private I2CDeviceInterface delegate;
	private int controller;
	private int address;
	private int addressSize;
	private ByteOrder byteOrder;

	/**
	 * @param controller I2C bus
	 * @param address    I2C device address
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(int controller, int address) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, I2CConstants.ADDR_SIZE_7,
				DEFAULT_BYTE_ORDER);
	}

	/**
	 * @param controller I2C bus
	 * @param address    I2C device address
	 * @param byteOrder  Default byte order for this device
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(int controller, int address, ByteOrder byteOrder) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, I2CConstants.ADDR_SIZE_7, byteOrder);
	}

	/**
	 * @param controller  I2C bus
	 * @param address     I2C device address
	 * @param addressSize I2C device address size. Can be 7 or 10
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(int controller, int address, int addressSize) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, addressSize, DEFAULT_BYTE_ORDER);
	}

	/**
	 * @param controller  I2C bus
	 * @param address     I2C device address
	 * @param addressSize I2C device address size. Can be 7 or 10
	 * @param byteOrder   Default byte order for this device
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public I2CDevice(int controller, int address, int addressSize, ByteOrder byteOrder) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, addressSize, byteOrder);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this device
	 * @param controller    I2C bus
	 * @param address       I2C device address
	 * @param addressSize   I2C device address size. Can be 7 or 10
	 * @param byteOrder     Default byte order for this device
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(I2CDeviceFactoryInterface deviceFactory, int controller, int address, int addressSize,
			ByteOrder byteOrder) throws RuntimeIOException {
		delegate = deviceFactory.provisionI2CDevice(controller, address, addressSize);

		this.controller = controller;
		this.address = address;
		this.addressSize = addressSize;
		this.byteOrder = byteOrder;
	}

	public int getController() {
		return controller;
	}

	public int getAddress() {
		return address;
	}

	public int getAddressSize() {
		return addressSize;
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public final boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		delegate.close();
	}

	public boolean probe() {
		return probe(ProbeMode.AUTO);
	}

	@Override
	public void writeQuick(byte bit) {
		synchronized (delegate) {
			delegate.writeQuick(bit);
		}
	}

	@Override
	public boolean probe(ProbeMode mode) {
		synchronized (delegate) {
			return delegate.probe(mode);
		}
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readByte();
		}
	}

	@Override
	public void writeByte(byte data) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeByte(data);
		}
	}

	/**
	 * Read a single byte from an 8-bit device register
	 * 
	 * @param register Register regAddr to read from
	 * @throws RuntimeIOException if an I/O error occurs
	 * @return the byte read
	 */
	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readByteData(register);
		}
	}

	/**
	 * Writes a single byte to a register
	 *
	 * @param register Register to write
	 * @param value    Byte to be written
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void writeByteData(int register, byte value) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeByteData(register, value);
		}
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		synchronized (delegate) {
			return readWordData(register);
		}
	}

	@Override
	public void writeWordData(int register, short value) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeWordData(register, value);
		}
	}

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readBytes(buffer);
		}
	}

	@Override
	public void writeBytes(byte[] data) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeBytes(data);
		}
	}

	@Override
	public short processCall(int register, short data) {
		synchronized (delegate) {
			return delegate.processCall(register, data);
		}
	}

	@Override
	public int readBlockData(int register, byte[] buffer) {
		synchronized (delegate) {
			return delegate.readBlockData(register, buffer);
		}
	}

	@Override
	public void writeBlockData(int register, byte[] data) {
		synchronized (delegate) {
			delegate.writeBlockData(register, data);
		}
	}

	@Override
	public byte[] blockProcessCall(int register, byte[] txData) {
		synchronized (delegate) {
			return delegate.blockProcessCall(register, txData);
		}
	}

	@Override
	public void readI2CBlockData(int register, byte[] buffer) {
		synchronized (delegate) {
			delegate.readI2CBlockData(register, buffer);
		}
	}

	@Override
	public void writeI2CBlockData(int register, byte[] data) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeI2CBlockData(register, data);
		}
	}

	//
	// Utility methods
	//

	public void writeByteData(int register, int data) throws RuntimeIOException {
		writeByteData(register, (byte) data);
	}

	public short readUByte(int register) throws RuntimeIOException {
		return (short) (readByteData(register) & 0xff);
	}

	public ByteBuffer readBytesAsByteBuffer(int length) {
		ByteBuffer buffer = ByteBuffer.wrap(readBytes(length));
		buffer.order(byteOrder);
		return buffer;
	}

	public void writeBytes(ByteBuffer buffer) {
		byte[] tx_buf = new byte[buffer.remaining()];
		buffer.put(tx_buf);
		writeBytes(tx_buf);
	}

	public byte[] readI2CBlockDataByteArray(int register, int length) {
		byte[] data = new byte[length];
		readI2CBlockData(register, data);
		return data;
	}

	public ByteBuffer readI2CBlockDataByteBuffer(int register, int length) {
		byte[] data = new byte[length];
		readI2CBlockData(register, data);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(byteOrder);
		return buffer;
	}

	public short readShort(int address) throws RuntimeIOException {
		return readI2CBlockDataByteBuffer(address, 2).getShort();
	}

	public int readUShort(int address) throws RuntimeIOException {
		return readShort(address) & 0xffff;
	}

	public long readInt(int address) throws RuntimeIOException {
		return readI2CBlockDataByteBuffer(address, 4).getInt();
	}

	public long readUInt(int address) throws RuntimeIOException {
		return readInt(address) & 0xffffffffL;
	}

	public long readUInt(int address, int numBytes) throws RuntimeIOException {
		if (numBytes > 4) {
			throw new IllegalArgumentException("Maximum int length is 4 bytes - you requested " + numBytes);
		}

		if (numBytes == 4) {
			return readUInt(address);
		}

		byte[] data = readI2CBlockDataByteArray(address, numBytes);

		long val = 0;
		for (int i = 0; i < numBytes; i++) {
			val |= (data[byteOrder == ByteOrder.LITTLE_ENDIAN ? numBytes - i - 1 : i] & 0xff) << (8
					* (numBytes - i - 1));
		}

		return val;
	}

	public byte[] readBytes(int length) {
		byte[] buffer = new byte[length];
		readBytes(buffer);
		return buffer;
	}

	public boolean readBit(int register, int bit) {
		return BitManipulation.isBitSet(readByteData(register), bit);
	}

	public void writeBit(int register, int bit, boolean value) {
		byte cur_val = readByteData(register);
		writeByteData(register, BitManipulation.setBitValue(cur_val, value, bit));
	}

	public byte[] readBytes(int register, int length) {
		byte[] buffer = new byte[length];
		readI2CBlockData(register, buffer);
		return buffer;
	}
}
