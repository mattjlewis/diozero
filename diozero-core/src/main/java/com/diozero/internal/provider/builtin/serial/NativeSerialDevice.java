package com.diozero.internal.provider.builtin.serial;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     NativeSerialDevice.java  
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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.tinylog.Logger;

import com.diozero.api.SerialDevice;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

public class NativeSerialDevice implements Closeable {
	private static final int FD_NOT_INITIALISED = -1;

	static {
		LibraryLoader.loadSystemUtils();
	}

	// See https://www.cmrr.umn.edu/~strupp/serial.html
	private static native FileDescriptor serialOpen(String device, int baud, int dataBits, int stopBits, int parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis);

	private static native int serialConfigPort(int fd, int baud, int dataBits, int stopBits, int parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis);
	// private static native int serialReadByte(int fd);
	// private static native int serialWriteByte(int fd, int bVal);
	// private static native int serialRead(int fd, byte[] buffer);
	// private static native int serialWrite(int fd, byte[] data);
	// private static native int serialBytesAvailable(int fd);
	private static native int serialClose(FileDescriptor fd);

	private FileDescriptor fileDescriptor;
	private FileInputStream inputStream;
	private FileOutputStream outputStream;
	private String deviceFile;

	/**
	 * Open a new serial device
	 * 
	 * @param deviceFile        Serial device filename including path, e.g. /dev/ttyACM0
	 * @param baud              Default is 9600.
	 * @param dataBits          The number of data bits to use per word; default is
	 *                          8, values from 5 to 8 are acceptable.
	 * @param stopBits          The number of stop bits; default is 1, but 2 bits
	 *                          can also be used or even 1.5 on Windows machines.
	 * @param parity            Specifies how error detection is carried out; valid
	 *                          values are NO_PARITY, EVEN_PARITY, ODD_PARITY,
	 *                          MARK_PARITY, and SPACE_PARITY
	 * @param readBlocking      Should blocking I/O be used for read operations?
	 * @param minReadChars      Minimum number of characters to read
	 * @param readTimeoutMillis Duration in milliseconds for read timeouts; must be
	 *                          in units of 100.
	 */
	public NativeSerialDevice(String deviceFile, int baud, SerialDevice.DataBits dataBits,
			SerialDevice.StopBits stopBits, SerialDevice.Parity parity, boolean readBlocking, int minReadChars,
			int readTimeoutMillis) {
		this.deviceFile = deviceFile;

		fileDescriptor = serialOpen(deviceFile, baud, dataBits.ordinal(), stopBits.ordinal(), parity.ordinal(),
				readBlocking, minReadChars, readTimeoutMillis);
		if (fileDescriptor == null) {
			throw new RuntimeIOException("Error opening serial device '" + deviceFile + "'");
		}

		inputStream = new FileInputStream(fileDescriptor);
		outputStream = new FileOutputStream(fileDescriptor);
	}

	public int read() {
		try {
			return inputStream.read();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in serial device read for '" + deviceFile + "': " + e.getMessage(), e);
		}
	}

	public byte readByte() {
		try {
			int read = inputStream.read();
			if (read == -1) {
				// TODO Need to check if we get this with non-blocking I/O and therefore should
				// not treat as an error...
				throw new RuntimeIOException("End of file reached");
			}
			return (byte) (read & 0xff);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in serial device readByte for '" + deviceFile + "': " + e.getMessage(),
					e);
		}

		// int rc = serialReadByte(fd);
		// if (rc < 0) {
		// throw new RuntimeIOException("Error in serial device readByte for '" +
		// deviceFile + "': " + rc);
		// }
		// return (byte) (rc & 0xff);
	}

	public void writeByte(byte value) {
		try {
			outputStream.write(value & 0xff);
			outputStream.flush();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in serial device writeByte for '" + deviceFile + "': " + e.getMessage(),
					e);
		}

		// int rc = serialWriteByte(fd, value & 0xff);
		// if (rc == -1) {
		// throw new RuntimeIOException("Error in serial device writeByte for '" +
		// deviceFile + "': " + rc);
		// }
	}

	public int read(byte[] buffer) {
		try {
			return inputStream.read(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in serial device read for '" + deviceFile + "': " + e.getMessage(), e);
		}

		// int rc = serialRead(fd, buffer);
		// if (rc < 0) {
		// throw new RuntimeIOException("Error in serial device read for '" + deviceFile
		// + "': " + rc);
		// }
	}

	public void write(byte[] data) {
		try {
			outputStream.write(data);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in serial device write for '" + deviceFile + "': " + e.getMessage(), e);
		}

		// int rc = serialWrite(fd, data);
		// if (rc < 0) {
		// throw new RuntimeIOException("Error in serial device write for '" +
		// deviceFile + "': " + rc);
		// }
	}

	public int bytesAvailable() {
		try {
			return inputStream.available();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in serial device bytesAvailable for '" + deviceFile + "'");
		}

		// int rc = serialBytesAvailable(fd);
		// if (rc < 0) {
		// throw new RuntimeIOException("Error in serial device bytesAvailable for '" +
		// deviceFile + "': " + rc);
		// }
		// return rc;
	}

	@Override
	public void close() {
		Logger.trace("closing...");

		if (fileDescriptor == null) {
			Logger.trace("Already closed...");
			return;
		}

		serialClose(fileDescriptor);

		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
			inputStream = null;
		}

		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
			}
			outputStream = null;
		}

		fileDescriptor = null;

		Logger.trace("closed");
	}

	public String getDeviceFile() {
		return deviceFile;
	}
}
