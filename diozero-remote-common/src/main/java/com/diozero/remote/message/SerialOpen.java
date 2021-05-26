package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Common
 * Filename:     SerialOpen.java
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

import com.diozero.api.SerialDevice;

public class SerialOpen extends SerialBase {
	private static final long serialVersionUID = -6845356597965548461L;

	private int baud;
	private SerialDevice.DataBits dataBits;
	private SerialDevice.StopBits stopBits;
	private SerialDevice.Parity parity;
	private boolean readBlocking;
	private int minReadChars;
	private int readTimeoutMillis;

	public SerialOpen(String deviceFile, int baud, SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits,
			SerialDevice.Parity parity, boolean readBlocking, int minReadChars, int readTimeoutMillis,
			String correlationId) {
		super(deviceFile, correlationId);

		this.baud = baud;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity = parity;
		this.readBlocking = readBlocking;
		this.minReadChars = minReadChars;
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public int getBaud() {
		return baud;
	}

	public SerialDevice.DataBits getDataBits() {
		return dataBits;
	}

	public SerialDevice.StopBits getStopBits() {
		return stopBits;
	}

	public SerialDevice.Parity getParity() {
		return parity;
	}

	public boolean isReadBlocking() {
		return readBlocking;
	}

	public int getMinReadChars() {
		return minReadChars;
	}

	public int getReadTimeoutMillis() {
		return readTimeoutMillis;
	}
}
