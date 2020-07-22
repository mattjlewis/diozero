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

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.I2CDeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.IOUtil;
import com.diozero.util.RuntimeIOException;

/**
 * Utility class reading / writing to I2C devices.
 */
public class I2CDevice implements Closeable, I2CConstants {
	public static enum ProbeMode {
		QUICK, READ, AUTO;
	}

	private I2CDeviceInterface device;
	private int controller;
	private int address;
	private int addressSize;
	private int clockFrequency;
	private ByteOrder order;

	/**
	 * @param controller
	 *            I2C bus
	 * @param address
	 *            I2C device address
	 * @throws RuntimeIOException
	 *             If an I/O error occurred
	 */
	public I2CDevice(int controller, int address) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY, IOUtil.DEFAULT_BYTE_ORDER);
	}

	/**
	 * @param controller
	 *            I2C bus
	 * @param address
	 *            I2C device address
	 * @param order
	 *            Default byte order for this device
	 * @throws RuntimeIOException
	 *             If an I/O error occurred
	 */
	public I2CDevice(int controller, int address, ByteOrder order) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY, order);
	}

	/**
	 * @param controller
	 *            I2C bus
	 * @param address
	 *            I2C device address
	 * @param addressSize
	 *            I2C device address size. Can be 7 or 10
	 * @param clockFrequency
	 *            I2C clock frequency
	 * @throws RuntimeIOException
	 *             If an I/O error occurred
	 */
	public I2CDevice(int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, addressSize, clockFrequency,
				IOUtil.DEFAULT_BYTE_ORDER);
	}

	/**
	 * @param controller
	 *            I2C bus
	 * @param address
	 *            I2C device address
	 * @param addressSize
	 *            I2C device address size. Can be 7 or 10
	 * @param clockFrequency
	 *            I2C clock frequency
	 * @param order
	 *            Default byte order for this device
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public I2CDevice(int controller, int address, int addressSize, int clockFrequency, ByteOrder order)
			throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, addressSize, clockFrequency, order);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to provision this device
	 * @param controller
	 *            I2C bus
	 * @param address
	 *            I2C device address
	 * @param addressSize
	 *            I2C device address size. Can be 7 or 10
	 * @param clockFrequency
	 *            I2C clock frequency
	 * @param order
	 *            Default byte order for this device
	 * @throws RuntimeIOException
	 *             If an I/O error occurred
	 */
	public I2CDevice(I2CDeviceFactoryInterface deviceFactory, int controller, int address, int addressSize,
			int clockFrequency, ByteOrder order) throws RuntimeIOException {
		device = deviceFactory.provisionI2CDevice(controller, address, addressSize, clockFrequency);

		this.controller = controller;
		this.address = address;
		this.addressSize = addressSize;
		this.clockFrequency = clockFrequency;
		this.order = order;
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

	public int getClockFrequency() {
		return clockFrequency;
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		device.close();
	}

	public boolean probe() {
		return probe(ProbeMode.AUTO);
	}

	public boolean probe(ProbeMode mode) {
		return device.probe(mode);
	}

	public final boolean isOpen() {
		return device.isOpen();
	}

	/**
	 * Writes a single byte to a register
	 *
	 * @param register
	 *            Register to write
	 * @param subAddressSize
	 *            sub-address size in bytes (1 or 2)
	 * @param value
	 *            Bytes to be written
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void write(int register, int subAddressSize, byte[] value) throws RuntimeIOException {
		synchronized (device) {
			device.writeI2CBlockData(register, subAddressSize, ByteBuffer.wrap(value));
		}
	}

	public void writeShort(int regAddr, short val) throws RuntimeIOException {
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		buffer.putShort(val);
		buffer.flip();
		synchronized (device) {
			device.writeI2CBlockData(regAddr, SUB_ADDRESS_SIZE_1_BYTE, buffer);
		}
	}

	public ByteBuffer read(int address, int count) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(count);
		read(address, buffer);
		return buffer;
	}

	public void read(int address, ByteBuffer dst) throws RuntimeIOException {
		read(address, SUB_ADDRESS_SIZE_1_BYTE, dst);
	}

	public void read(int address, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		synchronized (device) {
			device.readI2CBlockData(address, subAddressSize, buffer);
		}
		buffer.rewind();
		buffer.order(order);
	}

	public byte[] read(int address, int subAddressSize, int count) throws RuntimeIOException {
		ByteBuffer buffer = ByteBuffer.allocateDirect(subAddressSize * count);
		read(address, subAddressSize, buffer);

		byte[] data = new byte[count];
		buffer.get(data);

		return data;
	}

	/**
	 * Read a single byte from an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register regAddr to read from
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 * @return the byte read
	 */
	public byte readByte(int regAddr) throws RuntimeIOException {
		// int8_t I2Cdev::readByte(uint8_t devAddr, uint8_t regAddr, uint8_t
		// *data, uint16_t timeout)
		synchronized (device) {
			return device.readByteData(regAddr);
		}
	}

	public short readUByte(int regAddr) throws RuntimeIOException {
		return (short) (readByte(regAddr) & 0xff);
	}

	public byte readByte(int register, int subAddressSize) throws RuntimeIOException {
		synchronized (device) {
			return device.readByteData(register);
		}
	}

	public short readShort(int address) throws RuntimeIOException {
		return readShort(address, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, order);
	}

	public short readShort(int address, int subAddressSize) throws RuntimeIOException {
		return readShort(address, subAddressSize, order);
	}

	public short readShort(int address, int subAddressSize, ByteOrder order) throws RuntimeIOException {
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		read(address, subAddressSize, buffer);

		return buffer.getShort();
	}

	public int readUShort(int address) throws RuntimeIOException {
		return readUShort(address, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, order);
	}

	public int readUShort(int address, int subAddressSize) throws RuntimeIOException {
		return readUShort(address, subAddressSize, order);
	}

	public int readUShort(int address, int subAddressSize, ByteOrder order) throws RuntimeIOException {
		return readShort(address, subAddressSize, order) & 0xffff;
	}

	public long readUInt(int address) throws RuntimeIOException {
		return readUInt(address, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, 4, order);
	}

	public long readUInt(int address, ByteOrder order) throws RuntimeIOException {
		return readUInt(address, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, 4, order);
	}

	public long readUInt(int address, int subAddressSize, int length) throws RuntimeIOException {
		return readUInt(address, subAddressSize, length, order);
	}

	public long readUInt(int address, int subAddressSize, int length, ByteOrder order) throws RuntimeIOException {
		if (length > 4) {
			throw new IllegalArgumentException("Can't create an int for " + length + " bytes, max length is 4");
		}

		ByteBuffer buffer = ByteBuffer.allocateDirect(length);
		read(address, subAddressSize, buffer);

		return IOUtil.getUInt(buffer, length, order);
	}

	///////////////////////////////////////////////////////////////////////////////////
	// From
	/////////////////////////////////////////////////////////////////////////////////// https://github.com/jrowberg/i2cdevlib/blob/master/Arduino/I2Cdev/I2Cdev.cpp
	///////////////////////////////////////////////////////////////////////////////////

	/**
	 * Read a single bit from an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register regAddr to read from
	 * @param bitNum
	 *            Bit position to read (0-7)
	 * @return bit on/off value
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public boolean readBit(int regAddr, int bitNum) throws RuntimeIOException {
		// int8_t I2Cdev::readBit(uint8_t devAddr, uint8_t regAddr, uint8_t
		// bitNum, uint8_t *data, uint16_t timeout)
		byte b = readByte(regAddr);

		return (b & (1 << bitNum)) != 0;
	}

	/**
	 * Read multiple bits from an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register regAddr to read from
	 * @param bitStart
	 *            First bit position to read (0-7)
	 * @param length
	 *            Number of bits to read (not more than 8)
	 * @return the byte read
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public byte readBits(int regAddr, int bitStart, int length) throws RuntimeIOException {
		// int8_t I2Cdev::readBits(uint8_t devAddr, uint8_t regAddr, uint8_t
		// bitStart, uint8_t length, uint8_t *data, uint16_t timeout)
		byte b = readByte(regAddr);
		int mask = ((1 << length) - 1) << (bitStart - length + 1);
		b &= mask;
		b >>= (bitStart - length + 1);

		return b;
	}

	/**
	 * Read multiple bytes from an 8-bit device register
	 * 
	 * @param regAddr
	 *            First register regAddr to read from
	 * @param length
	 *            Number of bytes to read
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 * @return the bytes read
	 */
	public byte[] readBytes(int regAddr, int length) throws RuntimeIOException {
		// int8_t I2Cdev::readBytes(uint8_t devAddr, uint8_t regAddr, uint8_t
		// length, uint8_t *data, uint16_t timeout)
		return read(regAddr, SUB_ADDRESS_SIZE_1_BYTE, length);
	}

	/**
	 * write a single bit in an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register regAddr to write to
	 * @param bitNum
	 *            Bit position to write (0-7)
	 * @param value
	 *            New bit value to write
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeBit(int regAddr, int bitNum, int value) throws RuntimeIOException {
		// bool I2Cdev::writeBit(uint8_t devAddr, uint8_t regAddr, uint8_t
		// bitNum, uint8_t data)
		writeBit(regAddr, bitNum, value != 0);
	}

	/**
	 * write a single bit in an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register regAddr to write to
	 * @param bitNum
	 *            Bit position to write (0-7)
	 * @param value
	 *            New bit value to write
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeBit(int regAddr, int bitNum, boolean value) throws RuntimeIOException {
		// bool I2Cdev::writeBit(uint8_t devAddr, uint8_t regAddr, uint8_t
		// bitNum, uint8_t data)
		byte b = readByte(regAddr);
		b = (byte) (value ? (b | (1 << bitNum)) : (b & ~(1 << bitNum)));
		writeByte(regAddr, b);
	}

	/**
	 * Write multiple bits in an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register regAddr to write to
	 * @param bitStart
	 *            First bit position to write (0-7)
	 * @param length
	 *            Number of bits to write (not more than 8)
	 * @param data
	 *            Right-aligned value to write
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeBits(int regAddr, int bitStart, int length, int data) throws RuntimeIOException {
		// bool I2Cdev::writeBits(uint8_t devAddr, uint8_t regAddr, uint8_t
		// bitStart, uint8_t length, uint8_t data)
		// 010 value to write
		// 76543210 bit numbers
		// xxx args: bitStart=4, length=3
		// 00011100 mask byte
		// 10101111 original value (sample)
		// 10100011 original & ~mask
		// 10101011 masked | value
		int b = readByte(regAddr);
		int value = data;
		if (b != 0) {
			int mask = ((1 << length) - 1) << (bitStart - length + 1);
			value <<= (bitStart - length + 1); // shift data into correct
												// position
			value &= mask; // zero all non-important bits in data
			b &= ~(mask); // zero all important bits in existing byte
			b |= value; // combine data with existing byte
			writeByte(regAddr, b);
		}
	}

	/**
	 * Writes a single byte to a register
	 *
	 * @param register
	 *            Register to write
	 * @param subAddressSize
	 *            sub-address size in bytes (1 or 2)
	 * @param value
	 *            Byte to be written
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void write(int register, int subAddressSize, byte value) throws RuntimeIOException {
		synchronized (device) {
			device.writeByteData(register, value);
		}
	}

	/**
	 * Write a single byte to an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register address to write to
	 * @param data
	 *            New byte value to write
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeByte(int regAddr, int data) throws RuntimeIOException {
		// bool I2Cdev::writeByte(uint8_t devAddr, uint8_t regAddr, uint8_t
		// data)
		synchronized (device) {
			device.writeByteData(regAddr, (byte) data);
		}
	}

	/**
	 * Write a single byte to an 8-bit device register
	 * 
	 * @param regAddr
	 *            Register address to write to
	 * @param data
	 *            New byte value to write
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeByte(int regAddr, byte data) throws RuntimeIOException {
		// bool I2Cdev::writeByte(uint8_t devAddr, uint8_t regAddr, uint8_t
		// data)
		synchronized (device) {
			device.writeByteData(regAddr, data);
		}
	}

	/**
	 * Write a single word to a 16-bit device register
	 * 
	 * @param regAddr
	 *            Register address to write to
	 * @param data
	 *            New word value to write
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeWord(int regAddr, int data) throws RuntimeIOException {
		// bool I2Cdev::writeWord(uint8_t devAddr, uint8_t regAddr, uint16_t
		// data)
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		buffer.order(order);
		buffer.putShort((short) data);
		buffer.flip();

		byte[] array = new byte[buffer.remaining()];
		buffer.get(array);
		write(regAddr, SUB_ADDRESS_SIZE_2_BYTES, array);
	}

	/**
	 * Write multiple bytes to an 8-bit device register
	 * 
	 * @param regAddr
	 *            First register address to write to
	 * @param length
	 *            Number of bytes to write
	 * @param data
	 *            Buffer to copy new data from
	 * @throws RuntimeIOException
	 *             if an I/O error occurs
	 */
	public void writeBytes(int regAddr, int length, byte[] data) throws RuntimeIOException {
		writeBytes(regAddr, length, data, 0);
	}

	public void writeBytes(int regAddr, int length, byte[] data, int offset) throws RuntimeIOException {
		/*
		 * if (I2CDEV_SERIAL_DEBUG) { System.out.format(
		 * "I2C (0x%x) writing %d bytes to 0x%x...%n", devAddr, length, regAddr); }
		 */

		byte[] dest = new byte[length];
		System.arraycopy(data, offset, dest, 0, length);
		write(regAddr, SUB_ADDRESS_SIZE_1_BYTE, dest);
	}

	public void read(ByteBuffer dst) throws RuntimeIOException {
		dst.order(order);
		synchronized (device) {
			device.read(dst);
		}
		dst.rewind();
	}

	public byte readByte() throws RuntimeIOException {
		synchronized (device) {
			return device.readByte();
		}
	}

	public byte[] read(int count) throws RuntimeIOException {
		ByteBuffer buffer = ByteBuffer.allocateDirect(count);
		read(buffer);

		byte[] data = new byte[count];
		buffer.get(data);

		return data;
	}

	public void write(byte[] data) throws RuntimeIOException {
		write(data, order);
	}

	public void write(byte[] data, ByteOrder order) throws RuntimeIOException {
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(order);
		synchronized (device) {
			device.write(buffer);
		}
	}

	public void write(ByteBuffer buffer, int payloadLength, ByteOrder order) throws RuntimeIOException {
		buffer.rewind();
		int lim = buffer.limit();
		if (payloadLength <= lim) {
			buffer.limit(payloadLength);
		}
		buffer.order(order);
		synchronized (device) {
			device.write(buffer);
		}
		buffer.limit(lim);
	}

	public void write(ByteBuffer buffer, int payloadLength) throws RuntimeIOException {
		write(buffer, payloadLength, order);
	}

	public void write(int registerAddress, int addressSize, ByteBuffer buffer, int payloadLength, ByteOrder order)
			throws RuntimeIOException {
		buffer.rewind();
		int lim = buffer.limit();
		if (payloadLength <= lim) {
			buffer.limit(payloadLength);
		}
		buffer.order(order);
		synchronized (device) {
			device.writeI2CBlockData(registerAddress, addressSize, buffer);
		}
		buffer.limit(lim);
	}

	public void write(int registerAddress, ByteBuffer buffer, int payloadLength) throws RuntimeIOException {
		write(registerAddress, SUB_ADDRESS_SIZE_1_BYTE, buffer, payloadLength, order);
	}

	public void writeByte(byte data) throws RuntimeIOException {
		synchronized (device) {
			device.writeByte(data);
		}
	}
}
