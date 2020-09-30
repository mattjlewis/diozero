package com.diozero.internal.provider.sysfs;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     NativeI2CDeviceSMBus.java  
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

import org.tinylog.Logger;

import com.diozero.api.DeviceBusyException;
import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;

/**
 * <p>JNI wrapper of SMBus interface.</p>
 * <p>Reference <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel I2C dev interface</a>
 * and <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus Protocol</a>.</p>
 * <p>See <a href="https://github.com/bivab/smbus-cffi/blob/master/include/linux/i2c-dev.h">i2c-dev</a> for defintion of the inline functions.</p>
 * <p>See <a href="https://github.com/bivab/smbus-cffi/blob/master/smbus/smbus.py">Python CFFI implementation.</a></p>
 */
public class NativeI2CDeviceSMBus implements I2CSMBusInterface {
	private static final int CLOSED = -1;

	private int controller;
	private int deviceAddress;
	private int fd = CLOSED;
	private int funcs;

	public NativeI2CDeviceSMBus(int controller, int deviceAddress, boolean force)
			throws RuntimeIOException {
		this.controller = controller;
		this.deviceAddress = deviceAddress;
		String device_file = "/dev/i2c-" + controller;

		int rc = NativeI2C.smbusOpen(device_file, deviceAddress, force);
		if (rc < 0) {
			if (rc == -16) {
				throw new DeviceBusyException("Error, I2C device " + controller + "-0x"
						+ Integer.toHexString(deviceAddress) + " is busy");
			}
			throw new RuntimeIOException(rc);
		}
		fd = rc;

		rc = NativeI2C.getFuncs(fd);
		if (rc < 0) {
			close();
			throw new RuntimeIOException("Error reading I2C_FUNCS for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
		
		funcs = rc;
		Logger.debug("I2C_FUNCS for controller {}: 0x{}", Integer.toString(controller), Integer.toHexString(funcs));
	}

	@Override
	public void close() {
		NativeI2C.smbusClose(fd);
		fd = CLOSED;
	}
	
	@Override
	/*
	 * Code ported from <a href="https://fossies.org/dox/i2c-tools-3.1.2/i2cdetect_8c_source.html#l00080">i2c-tools</a>.
	 */
	public boolean probe(I2CDevice.ProbeMode mode) {
		int res;
		switch (mode) {
		case QUICK:
			/* This is known to corrupt the Atmel AT24RF08 EEPROM */
			res = NativeI2C.writeQuick(fd, NativeI2C.I2C_SMBUS_WRITE);
			break;
		case READ:
			/* This is known to lock SMBus on various write-only chips (mainly clock chips) */
			res = NativeI2C.readByte(fd);
			break;
		default:
			if ((deviceAddress >= 0x30 && deviceAddress <= 0x37) || (deviceAddress >= 0x50 && deviceAddress <= 0x5F) ||
					(funcs & NativeI2C.I2C_FUNC_SMBUS_QUICK) == 0) {
				res = NativeI2C.readByte(fd);
			} else {
				res = NativeI2C.writeQuick(fd, NativeI2C.I2C_SMBUS_WRITE);
			}
		}
		return res >= 0;
	}
	
	@Override
	public void writeQuick(byte bit) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_QUICK) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_QUICK isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.writeQuick(fd, bit);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeQuick for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}

	@Override
	public byte readByte() {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_READ_BYTE) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_BYTE isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.readByte(fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readByte for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return (byte) rc;
	}

	@Override
	public void writeByte(byte data) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_WRITE_BYTE) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_BYTE isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.writeByte(fd, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeByte for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}

	@Override
	public byte[] readBytes(int length) {
		/*
		 * byte[] data = new byte[length]; for (int i=0; i<length; i++) {
		 * data[i] = readByte(); }
		 * 
		 * return data;
		 */
		byte[] data = new byte[length];
		int rc = NativeI2C.readBytes(fd, length, data);
		if (rc < 0 || rc != length) {
			throw new RuntimeIOException("Error in SMBus.readBytes for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
		return data;
	}

	@Override
	public void writeBytes(byte[] data) {
		int rc = NativeI2C.writeBytes(fd, data.length, data);
		if (rc < 0 || rc != data.length) {
			throw new RuntimeIOException("Error in SMBus.writeBytes for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}

	@Override
	public byte readByteData(int registerAddress) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_READ_BYTE_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_BYTE_DATA isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.readByteData(fd, registerAddress);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readByteData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return (byte) rc;
	}

	@Override
	public void writeByteData(int registerAddress, byte data) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_WRITE_BYTE_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_BYTE_DATA isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.writeByteData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeByteData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}

	@Override
	public short readWordData(int registerAddress) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_READ_WORD_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_WORD_DATA isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.readWordData(fd, registerAddress);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readWordData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return (short) rc;
	}

	@Override
	public void writeWordData(int registerAddress, short data) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_WRITE_WORD_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_WORD_DATA isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.writeWordData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeWordData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}

	@Override
	public short processCall(int registerAddress, short data) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_PROC_CALL) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_PROC_CALL isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.processCall(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.processCall for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return (short) rc;
	}

	@Override
	public byte[] readBlockData(int registerAddress) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_READ_BLOCK_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_BLOCK_DATA isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		byte[] data = new byte[MAX_I2C_BLOCK_SIZE];
		int rc = NativeI2C.readBlockData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readBlockData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return data;
	}

	@Override
	public void writeBlockData(int registerAddress, byte[] data) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_WRITE_BLOCK_DATA) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_BLOCK_DATA isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.writeBlockData(fd, registerAddress, data.length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeBlockData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}

	@Override
	public byte[] blockProcessCall(int registerAddress, byte[] txData, int length) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_BLOCK_PROC_CALL) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_BLOCK_PROCESS_CALL isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		byte[] rx_data = new byte[length];
		int rc = NativeI2C.blockProcessCall(fd, registerAddress, txData.length, txData, rx_data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.blockProcessCall for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return rx_data;
	}

	@Override
	public byte[] readI2CBlockData(int registerAddress, int length) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_READ_I2C_BLOCK) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_READ_I2C_BLOCK isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		byte[] data = new byte[length];
		int rc = NativeI2C.readI2CBlockData(fd, registerAddress, length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readI2CBlockData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}

		return data;
	}

	@Override
	public void writeI2CBlockData(int registerAddress, byte[] data) {
		if ((funcs & NativeI2C.I2C_FUNC_SMBUS_WRITE_I2C_BLOCK) == 0) {
			Logger.warn("Function I2C_FUNC_SMBUS_WRITE_I2C_BLOCK isn't supported for device i2c-{}-0x{}",
					Integer.valueOf(controller), Integer.toHexString(deviceAddress));
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = NativeI2C.writeI2CBlockData(fd, registerAddress, data.length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeI2CBlockData for device i2c-" + controller + "-0x"
					+ Integer.toHexString(deviceAddress) + ": " + rc);
		}
	}
}
