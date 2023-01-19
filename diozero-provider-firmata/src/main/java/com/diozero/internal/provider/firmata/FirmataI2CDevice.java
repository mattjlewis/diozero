package com.diozero.internal.provider.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataI2CDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter.I2CResponse;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.util.PropertyUtil;

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
public class FirmataI2CDevice extends AbstractDevice implements InternalI2CDeviceInterface {
	// i2cConfig must be invoked at least once
	private static AtomicBoolean I2C_CONFIGURED = new AtomicBoolean();
	private static final int DEFAULT_I2C_DELAY = 0;
	private static final String I2C_DELAY_PROP = "diozero.firmata.i2cDelay";

	private FirmataAdapter adapter;
	private int address;
	private boolean autoRestart = false;
	private boolean addressSize10Bit;

	public FirmataI2CDevice(FirmataDeviceFactory deviceFactory, String key, int controller, int address,
			I2CConstants.AddressSize addressSize) {
		super(key, deviceFactory);

		adapter = deviceFactory.getFirmataAdapter();
		synchronized (I2C_CONFIGURED) {
			if (!I2C_CONFIGURED.getAndSet(true)) {
				adapter.i2cConfig(PropertyUtil.getIntProperty(I2C_DELAY_PROP, DEFAULT_I2C_DELAY));
			}
		}
		this.address = address;
		addressSize10Bit = addressSize == I2CConstants.AddressSize.SIZE_10;
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

		I2CResponse response = adapter.i2cRead(address, autoRestart, addressSize10Bit, 1);
		return response.getData()[0];
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		adapter.i2cWrite(address, autoRestart, addressSize10Bit, new byte[] { b });
	}

	@Override
	public byte readByteData(int register) {
		I2CResponse response = adapter.i2cReadData(address, autoRestart, addressSize10Bit, register, 1);
		return response.getData()[0];
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		adapter.i2cWriteData(address, autoRestart, addressSize10Bit, register, new byte[] { b });
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		I2CResponse response = adapter.i2cReadData(address, autoRestart, addressSize10Bit, register, 2);
		// SMBus assumes little endian
		return (short) (response.getData()[0] & 0xff | (response.getData()[1] << 8));
	}

	@Override
	public void writeWordData(int register, short data) throws RuntimeIOException {
		byte[] tx_data = new byte[3];
		// Note SMBus assumes little endian (LSB first)
		tx_data[0] = (byte) (data & 0xff);
		tx_data[1] = (byte) ((data >> 8) & 0xff);
		adapter.i2cWriteData(address, autoRestart, addressSize10Bit, register, tx_data);
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
		I2CResponse response = adapter.i2cReadData(address, autoRestart, addressSize10Bit, register, buffer.length);
		byte[] rx_data = response.getData();
		System.arraycopy(rx_data, 0, buffer, 0, rx_data.length);
		return rx_data.length;
	}

	@Override
	public void writeI2CBlockData(int register, byte... data) throws RuntimeIOException {
		adapter.i2cWriteData(address, autoRestart, addressSize10Bit, register, data);
	}

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		I2CResponse response = adapter.i2cRead(address, autoRestart, addressSize10Bit, buffer.length);
		byte[] rx_data = response.getData();

		System.arraycopy(rx_data, 0, buffer, 0, rx_data.length);
		return rx_data.length;
	}

	@Override
	public void writeBytes(byte... data) throws RuntimeIOException {
		adapter.i2cWrite(address, autoRestart, addressSize10Bit, data);
	}

	@Override
	public void readWrite(I2CMessage[] messages, byte[] buffer) {
		throw new UnsupportedOperationException("I2C readWrite not supported");
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
	}
}
