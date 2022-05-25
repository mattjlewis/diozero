package com.diozero.internal.provider.builtin.i2c;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     NativeI2C.java
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

import com.diozero.api.I2CDeviceInterface.I2CMessage;

public class NativeI2C {
	/* smbus_access read or write markers */
	public static final byte I2C_SMBUS_READ = 1;
	public static final byte I2C_SMBUS_WRITE = 0;

	// System Management Bus (SMBus) commands
	static native int smbusOpen(String adapter, int deviceAddress, boolean force);

	public static native int getFuncs(int fd);

	static native int selectSlave(int fd, int deviceAddress, boolean force);

	static native int writeQuick(int fd, byte bit);

	static native int readByte(int fd);

	static native int writeByte(int fd, byte value);

	static native int readByteData(int fd, int registerAddress);

	static native int writeByteData(int fd, int registerAddress, byte value);

	static native int readWordData(int fd, int registerAddress);

	static native int writeWordData(int fd, int registerAddress, short value);

	static native int readWordSwapped(int fd, int registerAddress);

	static native int writeWordSwapped(int fd, int registerAddress, short value);

	static native int processCall(int fd, int registerAddress, short value);

	static native int readBlockData(int fd, int registerAddress, byte[] rxData);

	static native int writeBlockData(int fd, int registerAddress, int txLength, byte[] txData);

	static native int blockProcessCall(int fd, int registerAddress, int txLength, byte[] txData, byte[] rxData);

	static native int readI2CBlockData(int fd, int registerAddress, int rxLength, byte[] rxData);

	static native int writeI2CBlockData(int fd, int registerAddress, int txLength, byte[] txData);

	static native int readBytes(int fd, int rxLength, byte[] rxData);

	static native int writeBytes(int fd, int txLength, byte[] txData);

	static native int readWrite(int fd, int deviceAddress, I2CMessage[] messages, byte[] buffer);

	static native void smbusClose(int fd);
}
