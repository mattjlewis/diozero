package com.diozero.internal.provider.sysfs;

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
import java.nio.ByteBuffer;

import org.tinylog.Logger;

import com.diozero.api.SerialDevice;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

public class NativeSerialDevice implements Closeable {
	static {
		LibraryLoader.loadLibrary(NativeSpiDevice.class, "diozero-system-utils");
	}

	private static native int serialOpen(String device, int baud, int dataBits, int stopBits, int parity);
	private static native int serialConfigPort(int fd, int baud, int dataBits, int stopBits, int parity);
	private static native int serialReadByte(int fd);
	private static native int serialWriteByte(int fd, int bVal);
	private static native int serialRead(int fd, byte[] buffer);
	private static native int serialWrite(int fd, byte[] data);
	private static native int serialBytesAvailable(int fd);
	private static native int serialClose(int fd);

	private int fd;
	private String tty;

	/**
	 * Open a new serial device
	 * 
	 * @param tty      Device name
	 * @param baud     Default is 9600.
	 * @param dataBits The number of data bits to use per word; default is 8, values
	 *                 from 5 to 8 are acceptable.
	 * @param parity   Specifies how error detection is carried out; valid values
	 *                 are NO_PARITY, EVEN_PARITY, ODD_PARITY, MARK_PARITY, and
	 *                 SPACE_PARITY
	 * @param stopBits The number of stop bits; default is 1, but 2 bits can also be
	 *                 used or even 1.5 on Windows machines.
	 */
	public NativeSerialDevice(String tty, int baud, SerialDevice.DataBits dataBits, SerialDevice.Parity parity,
			SerialDevice.StopBits stopBits) {
		this.tty = tty;
		int rc = serialOpen("/dev/" + tty, baud, dataBits.ordinal(), parity.ordinal(), stopBits.ordinal());
		if (rc < 0) {
			throw new RuntimeIOException("Error opening serial device '" + tty + "': " + rc);
		}
		fd = rc;
	}

	public byte readByte() {
		int rc = serialReadByte(fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error in serial device readByte for '" + tty + "': " + rc);
		}
		return (byte) (rc & 0xff);
	}

	public void writeByte(byte value) {
		int rc = serialWriteByte(fd, value & 0xff);
		if (rc == -1) {
			throw new RuntimeIOException("Error in serial device writeByte for '" + tty + "': " + rc);
		}
	}

	public void read(ByteBuffer dst) {
		byte[] buffer = new byte[dst.remaining()];
		int rc = serialRead(fd, buffer);
		if (rc < 0) {
			throw new RuntimeIOException("Error in serial device read for '" + tty + "': " + rc);
		}

		dst.put(buffer);
		dst.flip();
	}

	public void write(ByteBuffer src) {
		byte[] buffer = new byte[src.remaining()];
		src.get(buffer);
		int rc = serialWrite(fd, buffer);
		if (rc < 0) {
			throw new RuntimeIOException("Error in serial device write for '" + tty + "': " + rc);
		}
	}

	public int bytesAvailable() {
		int rc = serialBytesAvailable(fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error in serial device bytesAvailable for '" + tty + "': " + rc);
		}
		return rc;
	}

	@Override
	public void close() {
		int rc = serialClose(fd);
		if (rc < 0) {
			Logger.error("Error closing serial device {}: {}", tty, Integer.valueOf(rc));
		}
	}

	public String getTty() {
		return tty;
	}
}
