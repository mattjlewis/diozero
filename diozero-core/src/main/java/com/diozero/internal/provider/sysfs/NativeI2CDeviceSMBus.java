package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     NativeI2CDeviceSMBus.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import org.pmw.tinylog.Logger;

import com.diozero.util.RuntimeIOException;

/**
 * <p>JNI wrapper of SMBus interface.</p>
 * <p>Reference <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel I2C dev interface</a>
 * and <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus Protocol</a>.</p>
 * <p>See <a href="https://github.com/bivab/smbus-cffi/blob/master/include/linux/i2c-dev.h">i2c-dev</a> for defintion of the inline functions.</p>
 * <p>See <a href="https://github.com/bivab/smbus-cffi/blob/master/smbus/smbus.py">Python CFFI implementation.</a></p>
 */
public class NativeI2CDeviceSMBus implements I2CSMBusInterface {
	private static final int I2C_FUNC_I2C                    = 0x00000001;
	private static final int I2C_FUNC_10BIT_ADDR             = 0x00000002;
	private static final int I2C_FUNC_PROTOCOL_MANGLING      = 0x00000004; /* I2C_M_IGNORE_NAK etc. */
	private static final int I2C_FUNC_SMBUS_PEC              = 0x00000008;
	private static final int I2C_FUNC_NOSTART                = 0x00000010; /* I2C_M_NOSTART */
	private static final int I2C_FUNC_SMBUS_BLOCK_PROC_CALL  = 0x00008000; /* SMBus 2.0 */
	private static final int I2C_FUNC_SMBUS_QUICK            = 0x00010000;
	private static final int I2C_FUNC_SMBUS_READ_BYTE        = 0x00020000;
	private static final int I2C_FUNC_SMBUS_WRITE_BYTE       = 0x00040000;
	private static final int I2C_FUNC_SMBUS_READ_BYTE_DATA   = 0x00080000;
	private static final int I2C_FUNC_SMBUS_WRITE_BYTE_DATA  = 0x00100000;
	private static final int I2C_FUNC_SMBUS_READ_WORD_DATA   = 0x00200000;
	private static final int I2C_FUNC_SMBUS_WRITE_WORD_DATA  = 0x00400000;
	private static final int I2C_FUNC_SMBUS_PROC_CALL        = 0x00800000;
	private static final int I2C_FUNC_SMBUS_READ_BLOCK_DATA  = 0x01000000;
	private static final int I2C_FUNC_SMBUS_WRITE_BLOCK_DATA = 0x02000000;
	private static final int I2C_FUNC_SMBUS_READ_I2C_BLOCK   = 0x04000000; /* I2C-like block xfer  */
	private static final int I2C_FUNC_SMBUS_WRITE_I2C_BLOCK  = 0x08000000; /* w/ 1-byte reg. addr. */
	
	private static native int smbusOpen(String adapter, int deviceAddress, boolean force);
	private static native int getFuncs(int fd);
	private static native void smbusClose(int fd);
	
	private static native int writeQuick(int fd, byte value);
	private static native int readByte(int fd);
	private static native int writeByte(int fd, byte value);
	private static native int readBytes(int fd, int rxLength, byte[] rxData);
	private static native int writeBytes(int fd, int txLength, byte[] txData);
	private static native int readByteData(int fd, int registerAddress);
	private static native int writeByteData(int fd, int registerAddress, byte value);
	private static native int readWordData(int fd, int registerAddress);
	private static native int writeWordData(int fd, int registerAddress, short value);
	private static native int processCall(int fd, int registerAddress, short value);
	private static native int readBlockData(int fd, int registerAddress, byte[] rxData);
	private static native int writeBlockData(int fd, int registerAddress, int txLength, byte[] txData);
	private static native int readI2CBlockData(int fd, int registerAddress, int rxLength, byte[] rxData);
	private static native int writeI2CBlockData(int fd, int registerAddress, int txLength, byte[] txData);
	private static native int blockProcessCall(int fd, int registerAddress, int txLength, byte[] txData, byte[] rxData);
	
	private int controller;
	private int deviceAddress;
	private int fd;
	private int funcs;
	
	public NativeI2CDeviceSMBus(int controller, int deviceAddress, boolean force)
			throws I2CSMBusInterface.NotSupportedException {
		this.controller = controller;
		this.deviceAddress = deviceAddress;
		String device_file = "/dev/i2c-" + controller;
		
		fd = smbusOpen(device_file, deviceAddress, force);
		if (fd < 0) {
			throw new I2CSMBusInterface.NotSupportedException();
		}
		
		int rc = getFuncs(fd);
		if (rc < 0) {
			Logger.error("Error reading I2C_FUNCS: " + rc);
		} else {
			Logger.debug("I2C_FUNCS: 0x{}", Integer.toHexString(funcs));
		}
	}

	@Override
	public void close() {
		smbusClose(fd);
	}

	@Override
	public byte readByte() {
		if ((funcs & I2C_FUNC_SMBUS_READ_BYTE) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_READ_BYTE isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = readByte(fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readByte, rc=" + rc);
		}
		
		return (byte) rc;
	}

	@Override
	public void writeByte(byte data) {
		if ((funcs & I2C_FUNC_SMBUS_WRITE_BYTE) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_WRITE_BYTE isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = writeByte(fd, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeByte, rc=" + rc);
		}
	}
	
	@Override
	public byte[] readBytes(int length) {
		/*
		byte[] data = new byte[length];
		for (int i=0; i<length; i++) {
			data[i] = readByte();
		}
		
		return data;
		*/
		byte[] data = new byte[length];
		int rc = readBytes(fd, length, data);
		if (rc < 0 || rc != length) {
			throw new RuntimeIOException("Error in SMBus.readBytes, rc=" + rc);
		}
		return data;
	}
	
	@Override
	public void writeBytes(byte[] data) {
		int rc = writeBytes(fd, data.length, data);
		if (rc < 0 || rc != data.length) {
			throw new RuntimeIOException("Error in SMBus.writeBytes, rc=" + rc);
		}
	}

	@Override
	public byte readByteData(int registerAddress) {
		if ((funcs & I2C_FUNC_SMBUS_READ_BYTE_DATA) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_READ_BYTE_DATA isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = readByteData(fd, registerAddress);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readByteData, rc=" + rc);
		}
		
		return (byte) rc;
	}

	@Override
	public void writeByteData(int registerAddress, byte data) {
		if ((funcs & I2C_FUNC_SMBUS_WRITE_BYTE_DATA) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_WRITE_BYTE_DATA isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = writeByteData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeByteData, rc=" + rc);
		}
	}

	@Override
	public short readWordData(int registerAddress) {
		if ((funcs & I2C_FUNC_SMBUS_READ_WORD_DATA) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_READ_WORD_DATA isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = readWordData(fd, registerAddress);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readWordData, rc=" + rc);
		}
		
		return (short) rc;
	}

	@Override
	public void writeWordData(int registerAddress, short data) {
		if ((funcs & I2C_FUNC_SMBUS_WRITE_WORD_DATA) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_WRITE_WORD_DATA isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = writeWordData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeWordData, rc=" + rc);
		}
	}

	@Override
	public short processCall(int registerAddress, short data) {
		if ((funcs & I2C_FUNC_SMBUS_PROC_CALL) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_PROC_CALL isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = processCall(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.processCall, rc=" + rc);
		}
		
		return (short) rc;
	}

	@Override
	public byte[] readBlockData(int registerAddress) {
		if ((funcs & I2C_FUNC_SMBUS_READ_BLOCK_DATA) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_READ_BLOCK_DATA isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		byte[] data = new byte[MAX_I2C_BLOCK_SIZE];
		int rc = readBlockData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readI2CBlockData, rc=" + rc);
		}
		
		return data;
	}

	@Override
	public void writeBlockData(int registerAddress, byte[] data) {
		if ((funcs & I2C_FUNC_SMBUS_WRITE_BLOCK_DATA) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_WRITE_BLOCK_DATA isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = writeBlockData(fd, registerAddress, data.length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeI2CBlockData, rc=" + rc);
		}
	}

	@Override
	public byte[] blockProcessCall(int registerAddress, byte[] txData, int length) {
		if ((funcs & I2C_FUNC_SMBUS_BLOCK_PROC_CALL) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_BLOCK_PROCESS_CALL isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		byte[] rx_data = new byte[length];
		int rc = blockProcessCall(fd, registerAddress, txData.length, txData, rx_data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readBlockData, rc=" + rc);
		}
		
		return rx_data;
	}

	@Override
	public byte[] readI2CBlockData(int registerAddress, int length) {
		if ((funcs & I2C_FUNC_SMBUS_READ_I2C_BLOCK) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_READ_I2C_BLOCK isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		byte[] data = new byte[length];
		int rc = readI2CBlockData(fd, registerAddress, length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readI2CBlockData, rc=" + rc);
		}
		
		return data;
	}

	@Override
	public void writeI2CBlockData(int registerAddress, byte[] data) {
		if ((funcs & I2C_FUNC_SMBUS_WRITE_I2C_BLOCK) != 0) {
			Logger.error("Function I2C_FUNC_SMBUS_WRITE_I2C_BLOCK isn't supported");
			// TODO Throw an exception now or attempt anyway?
		}
		int rc = writeI2CBlockData(fd, registerAddress, data.length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeI2CBlockData, rc=" + rc);
		}
	}
}
