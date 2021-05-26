package com.diozero.internal.provider.pigpioj;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - pigpioj provider
 * Filename:     PigpioJSerialDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.util.SleepUtil;

import uk.pigpioj.PigpioInterface;

public class PigpioJSerialDevice extends AbstractDevice implements InternalSerialDeviceInterface {
	private static final int PI_BAD_HANDLE = -25;
	private static final int PI_SER_READ_FAILED = -86;
	private static final int PI_SER_READ_NO_DATA = -87;

	private static final long READ_DELAY = 10;
	private static final int CLOSED = -1;

	private PigpioInterface pigpioImpl;
	private int handle = CLOSED;
	private String deviceFile;
	private boolean readBlocking;
	private int minReadChars;
	private int readTimeoutMillis;

	public PigpioJSerialDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl,
			String deviceFile, int baud, SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits,
			SerialDevice.Parity parity, boolean readBlocking, int minReadChars, int readTimeoutMillis)
			throws RuntimeIOException {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.deviceFile = deviceFile;
		this.readBlocking = readBlocking;
		this.minReadChars = minReadChars;
		this.readTimeoutMillis = readTimeoutMillis;

		// Note flags must be 0 - dataBits, parity and stopBits are ignored
		int rc = pigpioImpl.serOpen(deviceFile, baud, 0);
		if (rc < 0) {
			handle = CLOSED;
			throw new RuntimeIOException(
					String.format("Error opening Serial device '%s', baud %d, flags %d, response: %d", deviceFile,
							Integer.valueOf(baud), Integer.valueOf(0), Integer.valueOf(rc)));
		}
		handle = rc;
		Logger.trace("Serial device ({}) opened, handle={}", deviceFile, Integer.valueOf(handle));
	}

	@Override
	public void writeByte(byte bVal) {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + deviceFile + " is closed");
		}

		int rc = pigpioImpl.serWriteByte(handle, bVal & 0xFF);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serWriteByte(), response: " + rc);
		}
	}

	@Override
	public int read() {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + deviceFile + " is closed");
		}

		int read;
		while (true) {
			// Returns PI_SER_READ_NO_DATA (-87) if there is no data available, can also
			// return PI_SER_READ_FAILED (-86)
			read = pigpioImpl.serReadByte(handle);
			if (read == PI_SER_READ_FAILED || read == PI_BAD_HANDLE) {
				throw new RuntimeIOException("Error in pigpioImpl.serReadByte - read failed");
			}
			
			if (read >= 0) {
				break;
			}
			
			if (!readBlocking) {
				read = SerialConstants.READ_TIMEOUT;
				break;
			}
			
			// FIXME Read timeouts
			
			SleepUtil.sleepMillis(READ_DELAY);
		}

		return read;
	}

	@Override
	public byte readByte() {
		return (byte) read();
	}

	@Override
	public void write(byte... data) {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + deviceFile + " is closed");
		}

		int rc = pigpioImpl.serWrite(handle, data, data.length);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serWrite(), response: " + rc);
		}
	}

	@Override
	public int read(byte[] buffer) {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + deviceFile + " is closed");
		}

		int read = 0;
		while (true) {
			byte[] read_buffer = new byte[buffer.length];
			// Returns PI_SER_READ_NO_DATA (-87) if there is no data available, can also
			// return PI_SER_READ_FAILED (-86)
			int rc = pigpioImpl.serRead(handle, read_buffer, read_buffer.length - read);

			if (rc == PI_SER_READ_FAILED || rc == PI_BAD_HANDLE) {
				throw new RuntimeIOException("Error in pigpioImpl.serRead - read failed");
			}

			if (rc > 0) {
				System.arraycopy(read_buffer, 0, buffer, read, rc);
				read += rc;
				
				if (read == buffer.length) {
					break;
				}
			}
			
			if (!readBlocking) {
				break;
			}

			// TODO Read timeouts and min read chars
			
			SleepUtil.sleepMillis(READ_DELAY);
		}

		return read;
	}

	@Override
	public int bytesAvailable() {
		if (!isOpen()) {
			throw new IllegalStateException("Serial Device " + deviceFile + " is closed");
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
			throw new IllegalStateException("Serial Device " + deviceFile + " is closed");
		}

		int rc = pigpioImpl.serClose(handle);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.serClose(), response: " + rc);
		}
	}
}
