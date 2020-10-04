package com.diozero.internal.provider.pigpioj;

import java.nio.ByteBuffer;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - pigpioj provider
 * Filename:     PigpioJSerialDevice.java  
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

import com.diozero.api.SerialDevice;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.util.RuntimeIOException;

import uk.pigpioj.PigpioInterface;

public class PigpioJSerialDevice extends AbstractDevice implements SerialDeviceInterface {
	private static final int CLOSED = -1;

	private PigpioInterface pigpioImpl;
	private int handle = CLOSED;
	private String tty;

	public PigpioJSerialDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl, String tty,
			int baud, SerialDevice.DataBits dataBits, SerialDevice.Parity parity, SerialDevice.StopBits stopBits)
			throws RuntimeIOException {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.tty = tty;

		// Note flags must be 0 - dataBits, parity and stopBits are ignored
		int rc = pigpioImpl.serOpen(tty, baud, 0);
		if (rc < 0) {
			handle = CLOSED;
			throw new RuntimeIOException(
					String.format("Error opening Serial device on tty %s, baud %d, flags %d, response: %d", tty,
							Integer.valueOf(baud), Integer.valueOf(0), Integer.valueOf(rc)));
		}
		handle = rc;
		Logger.trace("Serial device ({}) opened, handle={}", tty, Integer.valueOf(handle));
	}

	@Override
	public void writeByte(byte bVal) {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + tty + " is closed");
		}

		int rc = pigpioImpl.serWriteByte(handle, bVal & 0xFF);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serWriteByte(), response: " + rc);
		}
	}

	@Override
	public byte readByte() {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + tty + " is closed");
		}

		int rc = pigpioImpl.serReadByte(handle);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serReadByte(), response: " + rc);
		}

		return (byte) rc;
	}

	@Override
	public void write(ByteBuffer src) {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + tty + " is closed");
		}

		int to_write = src.remaining();
		byte[] buffer = new byte[to_write];
		src.get(buffer, src.position(), to_write);
		int rc = pigpioImpl.serWrite(handle, buffer, to_write);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serWrite(), response: " + rc);
		}
	}

	@Override
	public void read(ByteBuffer dst) {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + tty + " is closed");
		}

		int to_read = dst.remaining();
		byte[] buffer = new byte[to_read];
		int rc = pigpioImpl.serRead(handle, buffer, to_read);
		if (rc < 0 || rc != to_read) {
			throw new RuntimeIOException(
					"Didn't read correct number of bytes from serRead(), read " + rc + ", expected " + to_read);
		}
		dst.put(buffer);
		dst.flip();
	}

	@Override
	public int bytesAvailable() {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + tty + " is closed");
		}

		int rc = pigpioImpl.serDataAvailable(handle);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serDataAvailable(), response: " + rc);
		}

		return (byte) rc;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + tty + " is closed");
		}

		int rc = pigpioImpl.serClose(handle);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serClose(), response: " + rc);
		}
	}
}
