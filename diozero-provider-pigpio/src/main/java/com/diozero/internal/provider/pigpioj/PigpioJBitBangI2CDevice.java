package com.diozero.internal.provider.pigpioj;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - pigpioj provider
 * Filename:     PigpioJBitBangI2CDevice.java
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

import java.nio.ByteBuffer;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;

import uk.pigpioj.PigpioBitBangI2C;

public class PigpioJBitBangI2CDevice extends AbstractDevice {
	private int sda;
	private boolean open;

	public PigpioJBitBangI2CDevice(String key, DeviceFactoryInterface deviceFactory, int sda, int scl, int baud) {
		super(key, deviceFactory);

		this.sda = sda;

		int rc = PigpioBitBangI2C.bbI2COpen(sda, scl, baud);
		if (rc < 0) {
			throw new RuntimeIOException("Error in bbI2COpen(" + sda + ", " + scl + ", " + baud + "): " + rc);
		}
		open = true;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		int rc = PigpioBitBangI2C.bbI2CClose(sda);
		open = false;
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioBitBangI2C.bbI2CClose(" + sda + "), response: " + rc);
		}
	}

	public ByteBuffer bbI2CZip(ByteBuffer src, int readLen) throws RuntimeIOException {
		if (!isOpen()) {
			throw new IllegalStateException("BitBang I2C Device " + getKey() + " is closed");
		}

		int tx_count = src.remaining();
		byte[] tx = new byte[tx_count];
		src.get(tx);
		byte[] rx = new byte[readLen];
		int rc = PigpioBitBangI2C.bbI2CZip(sda, tx, tx_count, rx, readLen);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling bbI2CZip: " + rc);
		}

		return ByteBuffer.wrap(rx);
	}
}
