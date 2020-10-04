package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Provider
 * Filename:     RemoteSerialDevice.java  
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

import java.nio.ByteBuffer;

import com.diozero.api.SerialDevice;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class RemoteSerialDevice extends AbstractDevice implements SerialDeviceInterface {

	public RemoteSerialDevice(RemoteDeviceFactory deviceFactory, String key, String tty, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.Parity parity, SerialDevice.StopBits stopBits) {
		super(key, deviceFactory);

		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public boolean isOpen() {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public void writeByte(byte bVal) {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public byte readByte() {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public void write(ByteBuffer buffer) {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public void read(ByteBuffer buffer) {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public int bytesAvailable() {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}
}
