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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.internal.spi.I2CDeviceFactoryInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.BitManipulation;

/**
 * Utility class for interfacing with to I2C devices.
 * 
 * @see <a href="https://i2c.info/i2c-bus-specification">I2C Bus
 *      Specification</a>
 */
public class I2CDevice implements I2CDeviceInterface {
	public static enum ProbeMode {
		QUICK, READ, AUTO;
	}

	/**
	 * I2C device builder. Default values:
	 * <ul>
	 * <li>controller: 1</li>
	 * <li>addressSize: {@link I2CConstants.AddressSize#SIZE_7 7}</li>
	 * <li>byteOrder: {@link ByteOrder#BIG_ENDIAN Big Endian}</li>
	 * </ul>
	 */
	public static class Builder {
		/** Default to {@link ByteOrder#BIG_ENDIAN} */
		public static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

		private I2CDeviceFactoryInterface factory;
		private int controller = I2CConstants.CONTROLLER_1;
		private int address;
		private I2CConstants.AddressSize addressSize = I2CConstants.AddressSize.SIZE_7;
		private ByteOrder byteOrder = DEFAULT_BYTE_ORDER;

		protected Builder(int address) {
			this.address = address;
		}

		public I2CDeviceFactoryInterface getFactory() {
			return factory;
		}

		/**
		 * Set the I2C device factory to use for provisioning I2C device instances
		 * 
		 * @param factory the I2C device factory to use for provisioning I2C device
		 *                instances
		 * @return this builder instance
		 */
		public Builder setFactory(I2CDeviceFactoryInterface factory) {
			this.factory = factory;
			return this;
		}

		/**
		 * Set the I2C bus controller
		 * 
		 * @param controller the I2C bus controller number
		 *                   (<code>/dev/i2c-&lt;controller&gt;</code>)
		 * @return this builder instance
		 */
		public Builder setController(int controller) {
			this.controller = controller;
			return this;
		}

		/**
		 * Set the I2c device address
		 * 
		 * @param address the I2C device address
		 * @return this builder instance
		 */
		public Builder setAddress(int address) {
			this.address = address;
			return this;
		}

		/**
		 * Set the I2C device {@link I2CConstants.AddressSize address size}
		 * 
		 * @param addressSize the I2C device {@link I2CConstants.AddressSize address
		 *                    size}
		 * @return this builder instance
		 */
		public Builder setAddressSize(I2CConstants.AddressSize addressSize) {
			this.addressSize = addressSize;
			return this;
		}

		/**
		 * Set the Default {@link ByteOrder byte order} for this device
		 * 
		 * @param byteOrder the {@link ByteOrder byte order} that is only used in the
		 *                  additional non-SMBus I2C device utility methods
		 * @return this builder instance
		 */
		public Builder setByteOrder(ByteOrder byteOrder) {
			this.byteOrder = byteOrder;
			return this;
		}

		/**
		 * Construct a new I2CDevice instance
		 * 
		 * @return a new I2C device
		 */
		public I2CDevice build() {
			return new I2CDevice(factory == null ? DeviceFactoryHelper.getNativeDeviceFactory() : factory, controller,
					address, addressSize, byteOrder);
		}
	}

	/**
	 * Builder class for I2C devices
	 * 
	 * @param address the I2C device address
	 * @return I2C device builder
	 */
	public static Builder builder(int address) {
		return new Builder(address);
	}

	private InternalI2CDeviceInterface delegate;
	private int controller;
	private int address;
	private I2CConstants.AddressSize addressSize;
	private ByteOrder byteOrder;

	/**
	 * Use the default {@link I2CConstants.AddressSize#SIZE_7 7-bit} address size
	 * and {@link Builder#DEFAULT_BYTE_ORDER default} {@link ByteOrder byte
	 * order}
	 * 
	 * @see <a href="https://i2c.info/i2c-bus-specification">I2C Bus
	 *      Specification</a>
	 * 
	 * @param controller I2C bus controller number
	 * @param address    I2C device address
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(int controller, int address) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, I2CConstants.AddressSize.SIZE_7,
				Builder.DEFAULT_BYTE_ORDER);
	}

	/**
	 * Use the default {@link I2CConstants.AddressSize#SIZE_7 7-bit} address size
	 * 
	 * @see <a href="https://i2c.info/i2c-bus-specification">I2C Bus
	 *      Specification</a>
	 * 
	 * @param controller I2C bus controller number
	 * @param address    I2C device address
	 * @param byteOrder  The {@link ByteOrder byte order} that is only used in the
	 *                   additional non-SMBus I2C device utility methods
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(int controller, int address, ByteOrder byteOrder) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, I2CConstants.AddressSize.SIZE_7,
				byteOrder);
	}

	/**
	 * Use the {@link Builder#DEFAULT_BYTE_ORDER default} {@link ByteOrder byte
	 * order}
	 * 
	 * @see <a href="https://i2c.info/i2c-bus-specification">I2C Bus
	 *      Specification</a>
	 * 
	 * @param controller  I2C bus controller number
	 * @param address     I2C device address
	 * @param addressSize I2C device address size. Can be 7 or 10
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(int controller, int address, I2CConstants.AddressSize addressSize) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, addressSize,
				Builder.DEFAULT_BYTE_ORDER);
	}

	/**
	 * Use the default native device factory
	 * 
	 * @see <a href="https://i2c.info/i2c-bus-specification">I2C Bus
	 *      Specification</a>
	 * 
	 * @param controller  I2C bus controller number
	 * @param address     I2C device address
	 * @param addressSize I2C device address size. Can be 7 or 10
	 * @param byteOrder   the {@link ByteOrder byte order} that is only used in the
	 *                    additional non-SMBus I2C device utility methods
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public I2CDevice(int controller, int address, I2CConstants.AddressSize addressSize, ByteOrder byteOrder)
			throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), controller, address, addressSize, byteOrder);
	}

	/**
	 * Construct an I2C device using the specified I2C bus / controller, device
	 * address, address size and byte order. Note that the {@link ByteOrder byte
	 * order} is only used in the utility methods.
	 * 
	 * @see <a href="https://i2c.info/i2c-bus-specification">I2C Bus
	 *      Specification</a>
	 * 
	 * @param deviceFactory Device factory to use to provision this device
	 * @param controller    I2C bus controller number
	 * @param address       I2C device address
	 * @param addressSize   I2C device address size. Can be 7 or 10
	 * @param byteOrder     the {@link ByteOrder byte order} that is only used in
	 *                      the additional non-SMBus I2C device utility methods
	 * @throws RuntimeIOException If an I/O error occurred
	 */
	public I2CDevice(I2CDeviceFactoryInterface deviceFactory, int controller, int address,
			I2CConstants.AddressSize addressSize, ByteOrder byteOrder) throws RuntimeIOException {
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

	public I2CConstants.AddressSize getAddressSize() {
		return addressSize;
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		if (delegate.isOpen()) {
			delegate.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean probe(ProbeMode mode) {
		synchronized (delegate) {
			return delegate.probe(mode);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeQuick(byte bit) {
		synchronized (delegate) {
			delegate.writeQuick(bit);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte readByte() throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readByte();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeByte(byte data) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeByte(data);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readByteData(register);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeByteData(int register, byte value) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeByteData(register, value);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <strong>Note</strong> that the byte order for the returned word data is
	 * {@link ByteOrder#LITTLE_ENDIAN Little Endian} as per the SMBus specification,
	 * regardless of the {@link ByteOrder byte order} specified in the constructor
	 */
	@Override
	public short readWordData(int register) throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readWordData(register);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <strong>Note</strong> that the {@link ByteOrder byte order} for the input
	 * value is {@link ByteOrder#LITTLE_ENDIAN Little Endian} as per the SMBus
	 * specification, regardless of the {@link ByteOrder byte order} specified in
	 * the constructor
	 */
	@Override
	public void writeWordData(int register, short value) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeWordData(register, value);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <strong>Note</strong> that the byte order for the returned word data is
	 * {@link ByteOrder#BIG_ENDIAN Big Endian}, regardless of the {@link ByteOrder
	 * byte order} specified in the constructor
	 */
	@Override
	public short readWordSwapped(int register) throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readWordSwapped(register);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <strong>Note</strong> that the {@link ByteOrder byte order} for the input
	 * value is {@link ByteOrder#BIG_ENDIAN Big Endian}, regardless of the
	 * {@link ByteOrder byte order} specified in the constructor
	 */
	@Override
	public void writeWordSwapped(int register, short value) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeWordSwapped(register, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public short processCall(int register, short data) {
		synchronized (delegate) {
			return delegate.processCall(register, data);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] readBlockData(int register) {
		synchronized (delegate) {
			return delegate.readBlockData(register);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeBlockData(int register, byte... data) {
		synchronized (delegate) {
			delegate.writeBlockData(register, data);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] blockProcessCall(int register, byte... txData) {
		synchronized (delegate) {
			return delegate.blockProcessCall(register, txData);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int readI2CBlockData(int register, byte[] buffer) {
		synchronized (delegate) {
			return delegate.readI2CBlockData(register, buffer);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeI2CBlockData(int register, byte... data) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeI2CBlockData(register, data);
		}
	}

	//
	// Diozero extension methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		synchronized (delegate) {
			return delegate.readBytes(buffer);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeBytes(byte... data) throws RuntimeIOException {
		synchronized (delegate) {
			delegate.writeBytes(data);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int readNoStop(byte registerAddress, int rxLength, byte[] rxData, boolean repeatedStart) {
		synchronized (delegate) {
			return delegate.readNoStop(registerAddress, rxLength, rxData, repeatedStart);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void readWrite(I2CMessage[] messages, byte[] buffer) {
		// TODO Validate that buffer is big enough
		synchronized (delegate) {
			delegate.readWrite(messages, buffer);
		}
	}

	//
	// I2CDevice utility methods
	//

	/**
	 * Utility method that simply casts the int data parameter to byte and calls
	 * {@link I2CDevice#writeByteData(int, byte)}
	 * 
	 * @see I2CDevice#writeByteData(int, byte)
	 * 
	 * @param register the register to write to
	 * @param data     value to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void writeByteData(int register, int data) throws RuntimeIOException {
		writeByteData(register, (byte) data);
	}

	/**
	 * Utility method that simply converts the response from
	 * {@link I2CDevice#readByteData(int)} to an unsigned byte. A short is returned
	 * to ensure that the returned value is unsigned
	 * 
	 * @see I2CDevice#readByteData(int)
	 * 
	 * @param register the register to read from
	 * @return byte data returned converted to unsigned byte (represented as a
	 *         short)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public short readUByte(int register) throws RuntimeIOException {
		return (short) (readByteData(register) & 0xff);
	}

	/**
	 * Utility method that wraps the response from {@link I2CDevice#readBytes(int)}
	 * in a {@link ByteBuffer} that is configured to use the {@link ByteOrder byte
	 * order} specified in the constructor.
	 * 
	 * @see I2CDevice#readBytes(int)
	 * @see ByteBuffer#wrap(byte[])
	 * 
	 * @param length number of bytes to read
	 * @return A {@link ByteBuffer} containing the bytes read using the byte order
	 *         specified in the constructor
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public ByteBuffer readBytesAsByteBuffer(int length) throws RuntimeIOException {
		ByteBuffer buffer = ByteBuffer.wrap(readBytes(length));
		buffer.order(byteOrder);
		return buffer;
	}

	/**
	 * Utility method that wraps {@link I2CDevice#writeBytes(byte[])} to write the
	 * available bytes in the specified {@link ByteBuffer ByteBuffer}. The byte
	 * order of data in the ByteBuffer is unchanged.
	 * 
	 * @see I2CDevice#writeBytes(byte[])
	 * @see ByteBuffer#put(byte[])
	 * 
	 * @param buffer the {@link ByteBuffer} containing the data to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void writeBytes(ByteBuffer buffer) throws RuntimeIOException {
		byte[] tx_buf = new byte[buffer.remaining()];
		buffer.put(tx_buf);
		writeBytes(tx_buf);
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readI2CBlockData(int, byte[])} to
	 * read the specified number of bytes and return as a new byte array.
	 * 
	 * @see I2CDevice#readI2CBlockData(int, byte[])
	 * 
	 * @param register the register to read from
	 * @param length   the number of bytes to read
	 * @return the data read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public byte[] readI2CBlockDataByteArray(int register, int length) throws RuntimeIOException {
		byte[] data = new byte[length];
		int read = readI2CBlockData(register, data);
		if (read == length) {
			return data;
		}

		byte[] rx_data = new byte[read];
		System.arraycopy(data, 0, rx_data, 0, read);
		return rx_data;
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readI2CBlockData(int, byte[])} to
	 * read the specified number of bytes and return as a {@link ByteBuffer} using
	 * the {@link ByteOrder byte order} specified in the device constructor.
	 * 
	 * @see I2CDevice#readI2CBlockData(int, byte[])
	 * @see ByteBuffer#wrap(byte[])
	 * 
	 * @param register the register to read from
	 * @param length   the number of bytes to read
	 * @return the data read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public ByteBuffer readI2CBlockDataByteBuffer(int register, int length) throws RuntimeIOException {
		byte[] data = new byte[length];

		readI2CBlockData(register, data);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(byteOrder);

		return buffer;
	}

	/**
	 * Utility method that invokes either {@link I2CDevice#readWordData(int)} or
	 * {@link I2CDevice#readWordSwapped(int)} to read a signed short value from the
	 * requested register in the {@link ByteOrder byte order} specified in the
	 * constructor.
	 * 
	 * @see I2CDevice#readWordData(int)
	 * @see I2CDevice#readWordSwapped(int)
	 * 
	 * @param register register to read from
	 * @return the signed short value read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public short readShort(int register) throws RuntimeIOException {
		if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			return readWordData(register);
		}
		return readWordSwapped(register);
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readShort(int)} to read an
	 * unsigned short value from the requested register using the {@link ByteOrder
	 * byte order} specified in the constructor.
	 * 
	 * @see I2CDevice#readShort(int)
	 * 
	 * @param register register to read from
	 * @return the unsigned short value read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public int readUShort(int register) throws RuntimeIOException {
		return readShort(register) & 0xffff;
	}

	/**
	 * Utility method that wraps
	 * {@link I2CDevice#readI2CBlockDataByteBuffer(int, int)} to read a signed int
	 * value from the requested register using the {@link ByteOrder byte order}
	 * specified in the constructor.
	 * 
	 * @see I2CDevice#readI2CBlockDataByteBuffer(int, int)
	 * 
	 * @param register register to read from
	 * @return the signed int value read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public int readInt(int register) throws RuntimeIOException {
		return readI2CBlockDataByteBuffer(register, 4).getInt();
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readInt(int)} to read an unsigned
	 * int value from the requested register using the {@link ByteOrder byte order}
	 * specified in the constructor.
	 * 
	 * @see I2CDevice#readInt(int)
	 * 
	 * @param register register to read from
	 * @return the unsigned int value read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public long readUInt(int register) throws RuntimeIOException {
		return readInt(register) & 0xffffffffL;
	}

	/**
	 * Utility method that wraps
	 * {@link I2CDevice#readI2CBlockDataByteArray(int, int)} to read an unsigned int
	 * value on the specified length from the requested register using the
	 * {@link ByteOrder byte order} specified in the constructor.
	 * 
	 * @see I2CDevice#readI2CBlockDataByteArray(int, int)
	 * 
	 * @param register register to read from
	 * @param numBytes number of bytes to read (1..4)
	 * @return the unsigned int value read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public long readUInt(int register, int numBytes) throws RuntimeIOException {
		if (numBytes < 1 || numBytes > 4) {
			throw new IllegalArgumentException("Maximum int length is 4 bytes - you requested " + numBytes);
		}

		if (numBytes == 4) {
			return readUInt(address);
		}

		byte[] data = readI2CBlockDataByteArray(register, numBytes);

		long val = 0;
		for (int i = 0; i < numBytes; i++) {
			val |= (data[byteOrder == ByteOrder.LITTLE_ENDIAN ? numBytes - i - 1 : i] & 0xff) << (8
					* (numBytes - i - 1));
		}

		return val;
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readBytes(byte[])} to read the
	 * specified number of bytes.
	 * 
	 * @see I2CDevice#readBytes(byte[])
	 * 
	 * @param length the number of bytes to read
	 * @return the bytes read from the device
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public byte[] readBytes(int length) throws RuntimeIOException {
		byte[] buffer = new byte[length];
		readBytes(buffer);
		return buffer;
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readByteData(int)} to check if the
	 * specified bit number is set.
	 * 
	 * @see BitManipulation#isBitSet(byte, int)
	 * @see I2CDevice#readByteData(int)
	 * 
	 * @param register the register to read
	 * @param bit      the bit number to check
	 * @return true if the specified bit number is set
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public boolean readBit(int register, int bit) throws RuntimeIOException {
		return BitManipulation.isBitSet(readByteData(register), bit);
	}

	/**
	 * Utility method that wraps {@link I2CDevice#readByteData(int)} and
	 * {@link I2CDevice#writeByteData(int, byte)} to update the specified bit number
	 * 
	 * @see I2CDevice#readByteData(int)
	 * @see BitManipulation#setBitValue(byte, boolean, int)
	 * 
	 * @param register the register to update
	 * @param bit      the bit number to set
	 * @param value    the value to set the bit to
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void writeBit(int register, int bit, boolean value) throws RuntimeIOException {
		byte cur_val = readByteData(register);
		writeByteData(register, BitManipulation.setBitValue(cur_val, value, bit));
	}
}
