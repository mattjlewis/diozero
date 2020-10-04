package com.diozero.internal.provider.sysfs;

import java.nio.ByteBuffer;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SysFsSerialDevice.java  
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

public class SysFsSerialDevice extends AbstractDevice implements SerialDeviceInterface {
	private NativeSerialDevice device;

	public SysFsSerialDevice(DeviceFactoryInterface deviceFactory, String key, String tty, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.Parity parity, SerialDevice.StopBits stopBits) {
		super(key, deviceFactory);

		device = new NativeSerialDevice(tty, baud, dataBits, parity, stopBits);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		device.close();
	}

	@Override
	public byte readByte() {
		return device.readByte();
	}

	@Override
	public void writeByte(byte bVal) {
		device.writeByte(bVal);
	}

	@Override
	public void read(ByteBuffer dst) {
		device.read(dst);
	}

	@Override
	public void write(ByteBuffer src) {
		device.write(src);
	}

	@Override
	public int bytesAvailable() {
		return device.bytesAvailable();
	}
}
