package com.diozero.internal.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SerialDeviceFactoryInterface.java
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

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialDevice;

public interface SerialDeviceFactoryInterface extends DeviceFactoryInterface {
	String SERIAL_PREFIX = "-Serial-";

	/**
	 * Provision a serial device.
	 *
	 * @param deviceFilename    The O/S name of the device, e.g. "/dev/ttyUSB0"
	 * @param baud              Baud rate
	 * @param dataBits          Number of data bits
	 * @param stopBits          Number of stop bits
	 * @param parity            Parity option
	 * @param readBlocking      Do read operations block?
	 * @param minReadChars      The minimum number of characters to read
	 * @param readTimeoutMillis How long a read operation waits for data before
	 *                          timing out. 0 == no timeout
	 * @return Serial device instance
	 * @throws RuntimeIOException if an error occurs
	 */
	default InternalSerialDeviceInterface provisionSerialDevice(String deviceFilename, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {

		return registerDevice(()->createSerialKey(deviceFilename),
							  (k)-> createSerialDevice(k, deviceFilename, baud, dataBits, stopBits, parity,
													   readBlocking, minReadChars, readTimeoutMillis));
	}

	InternalSerialDeviceInterface createSerialDevice(String key, String deviceFilename, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException;

	static String createSerialKey(String keyPrefix, String deviceFilename) {
		return keyPrefix + SERIAL_PREFIX + deviceFilename;
	}
}
