package com.diozero.internal.provider.builtin.i2c;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     NativeI2CDeviceSMBus.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DeviceBusyException;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.I2CException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.util.PropertyUtil;

/**
 * <p>
 * JNI wrapper of SMBus interface.
 * </p>
 * <p>
 * Reference
 * <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel
 * I2C dev interface</a> and
 * <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus
 * Protocol</a>.
 * </p>
 * <p>
 * See <a href=
 * "https://github.com/torvalds/linux/blob/v5.4/include/linux/i2c.h">i2c-dev</a>
 * for a definition of the in-line functions.
 * </p>
 */
public class NativeI2CDeviceSMBus extends AbstractDevice implements InternalI2CDeviceInterface {
	private static final int CLOSED = -1;

	private static final int EAGAIN = -11;
	private static final int ETIMEDOUT = -110;
	private static final int EREMOTEIO = -121;

	private int controller;
	private int deviceAddress;
	private int fd = CLOSED;
	private int funcs;
	private int numRetries;

	public NativeI2CDeviceSMBus(DeviceFactoryInterface deviceFactory, String key, int controller, int address,
			I2CConstants.AddressSize addressSize, boolean force) {
		super(key, deviceFactory);

		this.controller = controller;
		this.deviceAddress = address;
		String device_file = "/dev/i2c-" + controller;

		numRetries = PropertyUtil.getIntProperty("diozero.i2c.retryCount", 2);

		Logger.debug("opening device {}", key);

		// TODO Support for 10-bit address sizing
		int rc = NativeI2C.smbusOpen(device_file, deviceAddress, force);
		if (rc < 0) {
			if (rc == -16) {
				throw new DeviceBusyException("Error, I2C device " + getKey() + " is busy");
			}
			throw new I2CException("Error opening I2C device: " + rc, rc);
		}
		fd = rc;

		rc = NativeI2C.getFuncs(fd);
		if (rc < 0) {
			close();
			throw new I2CException("Error reading I2C_FUNCS for device " + getKey() + ": " + rc, rc);
		}

		funcs = rc;
		Logger.debug("I2C_FUNCS for controller {}: 0x{}", Integer.toString(controller), Integer.toHexString(funcs));
	}

	public int getController() {
		return controller;
	}

	public int getDeviceAddress() {
		return deviceAddress;
	}

	@Override
	public void closeDevice() {
		Logger.trace("closeDevice {}", getKey());
		NativeI2C.smbusClose(fd);
		fd = CLOSED;
	}

	@Override
	/*
	 * Code ported from <a href=
	 * "https://fossies.org/dox/i2c-tools-3.1.2/i2cdetect_8c_source.html#l00080">i2c
	 * -tools</a>.
	 */
	public boolean probe(I2CDevice.ProbeMode mode) {
		int res;
		switch (mode) {
		case QUICK:
			/* This is known to corrupt the Atmel AT24RF08 EEPROM */
			res = NativeI2C.writeQuick(fd, NativeI2C.I2C_SMBUS_WRITE);
			break;
		case READ:
			/*
			 * This is known to lock SMBus on various write-only chips (mainly clock chips)
			 */
			res = NativeI2C.readByte(fd);
			break;
		default:
			if ((deviceAddress >= 0x30 && deviceAddress <= 0x37) || (deviceAddress >= 0x50 && deviceAddress <= 0x5F)
					|| (funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_QUICK) == 0) {
				res = NativeI2C.readByte(fd);
			} else {
				res = NativeI2C.writeQuick(fd, NativeI2C.I2C_SMBUS_WRITE);
			}
		}
		return res >= 0;
	}

	@Override
	public void writeQuick(byte bit) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_QUICK) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_QUICK isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_QUICK isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeQuick(fd, bit);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeQuick for device " + getKey() + ": " + rc, rc);
		}
	}

	@Override
	public byte readByte() throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BYTE) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_BYTE isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_READ_BYTE isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readByte(fd);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.readByte for device " + getKey() + ": " + rc, rc);
		}

		return (byte) rc;
	}

	@Override
	public void writeByte(byte data) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BYTE) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_BYTE isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_WRITE_BYTE isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeByte(fd, data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeByte for device " + getKey() + ": " + rc, rc);
		}
	}

	@Override
	public byte readByteData(int registerAddress) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BYTE_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_BYTE_DATA isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_READ_BYTE_DATA isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readByteData(fd, registerAddress);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.readByteData for device " + getKey() + ": " + rc, rc);
		}

		return (byte) rc;
	}

	@Override
	public void writeByteData(int registerAddress, byte data) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BYTE_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_BYTE_DATA isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_WRITE_BYTE_DATA isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeByteData(fd, registerAddress, data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeByteData for device " + getKey() + ": " + rc, rc);
		}
	}

	@Override
	public short readWordData(int registerAddress) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_READ_WORD_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_WORD_DATA isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_READ_WORD_DATA isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readWordData(fd, registerAddress);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.readWordData for device " + getKey() + ": " + rc, rc);
		}

		return (short) rc;
	}

	@Override
	public void writeWordData(int registerAddress, short data) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_WORD_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_WORD_DATA isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_WRITE_WORD_DATA isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeWordData(fd, registerAddress, data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeWordData for device " + getKey() + ": " + rc, rc);
		}
	}

	/*-
	@Override
	public short readWordSwapped(int registerAddress) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_READ_WORD_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_WORD_DATA isn't supported for device {}",
					getKey());
			throw new UnsupportedOperationException(
				"Function I2C_FUNC_SMBUS_READ_WORD_DATA isn't supported for device " + getKey());
		}
	
		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readWordSwapped(fd, registerAddress);
		}
	
		if (rc < 0) {
			throw new I2CException("Error in SMBus.readWordSwapped for device "
					+ getKey() + ": " + rc, rc);
		}
	
		return (short) rc;
	}
	
	@Override
	public void writeWordSwapped(int registerAddress, short data) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_WORD_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_WORD_DATA isn't supported for device {}",
					getKey());
			throw new UnsupportedOperationException(
				"Function I2C_FUNC_SMBUS_WRITE_WORD_DATA isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeWordSwapped(fd, registerAddress, data);
		}
	
		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeWordSwapped for device "
					+ getKey() + ": " + rc, rc);
		}
	}
	*/

	@Override
	public short processCall(int registerAddress, short data) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_PROC_CALL) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_PROC_CALL isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_PROC_CALL isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.processCall(fd, registerAddress, data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.processCall for device " + getKey() + ": " + rc, rc);
		}

		return (short) rc;
	}

	@Override
	public byte[] readBlockData(int registerAddress) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BLOCK_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_BLOCK_DATA isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_READ_BLOCK_DATA isn't supported for device " + getKey());
		}

		byte[] buffer = new byte[MAX_I2C_BLOCK_SIZE];

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readBlockData(fd, registerAddress, buffer);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.readBlockData for device " + getKey() + ": " + rc, rc);
		}

		byte[] rx_data = new byte[rc];
		System.arraycopy(buffer, 0, rx_data, 0, rc);

		return rx_data;
	}

	@Override
	public void writeBlockData(int registerAddress, byte... data)
			throws I2CException, UnsupportedOperationException, IllegalArgumentException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BLOCK_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_BLOCK_DATA isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_WRITE_BLOCK_DATA isn't supported for device " + getKey());
		}

		if (data.length > MAX_I2C_BLOCK_SIZE) {
			throw new IllegalArgumentException("Error in SMBus.writeBlockData for device " + getKey()
					+ ": array length must be <= 32, is " + data.length);
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeBlockData(fd, registerAddress, data.length, data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeBlockData for device " + getKey() + ": " + rc, rc);
		}
	}

	@Override
	public byte[] blockProcessCall(int registerAddress, byte... txData)
			throws I2CException, UnsupportedOperationException, IllegalArgumentException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_BLOCK_PROC_CALL) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_BLOCK_PROC_CALL isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_BLOCK_PROC_CALL isn't supported for device " + getKey());
		}

		if (txData.length > MAX_I2C_BLOCK_SIZE) {
			throw new IllegalArgumentException("Error in SMBus.blockProcessCall for device " + getKey()
					+ ": array length must be <= 32, is " + txData.length);
		}

		byte[] rx_data = new byte[txData.length];

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.blockProcessCall(fd, registerAddress, txData.length, txData, rx_data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.blockProcessCall for device " + getKey() + ": " + rc, rc);
		}

		return rx_data;
	}

	@Override
	public int readI2CBlockData(int registerAddress, byte[] buffer) throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_READ_I2C_BLOCK) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_I2C_BLOCK isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_READ_I2C_BLOCK isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readI2CBlockData(fd, registerAddress, buffer.length, buffer);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.readI2CBlockData for device " + getKey() + ": " + rc, rc);
		}

		return rc;
	}

	@Override
	public void writeI2CBlockData(int registerAddress, byte... data)
			throws I2CException, UnsupportedOperationException {
		if ((funcs & I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_I2C_BLOCK) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_I2C_BLOCK isn't supported for device {}", getKey());
			throw new UnsupportedOperationException(
					"Function I2C_FUNC_SMBUS_WRITE_I2C_BLOCK isn't supported for device " + getKey());
		}

		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeI2CBlockData(fd, registerAddress, data.length, data);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.writeI2CBlockData for device " + getKey() + ": " + rc, rc);
		}
	}

	@Override
	public int readBytes(byte[] buffer) throws I2CException {
		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readBytes(fd, buffer.length, buffer);
		}

		if (rc < 0) {
			throw new I2CException("Error in SMBus.readBytes for device " + getKey() + ": " + rc, rc);
		}

		return rc;
	}

	@Override
	public void writeBytes(byte... data) throws I2CException {
		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.writeBytes(fd, data.length, data);
		}

		if (rc < 0 || rc < data.length) {
			throw new I2CException("Error in SMBus.writeBytes for device " + getKey() + ": " + rc, rc);
		}
	}

	@Override
	public void readWrite(I2CMessage[] messages, byte[] buffer) throws I2CException {
		int rc = EAGAIN;
		for (int i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT); i++) {
			rc = NativeI2C.readWrite(fd, deviceAddress, messages, buffer);
		}

		if (rc < 0) {
			throw new I2CException("Error in I2C readWrite for device " + getKey() + ": " + rc, rc);
		}
	}
}
