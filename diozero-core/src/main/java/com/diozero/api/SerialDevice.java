package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SerialDevice.java  
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
import java.io.IOException;
import java.nio.ByteBuffer;

import org.tinylog.Logger;

import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.util.DeviceFactoryHelper;

public class SerialDevice implements SerialConstants, Closeable {
	private SerialDeviceInterface device;
	
	public SerialDevice(String deviceName) {
		this(deviceName, DEFAULT_BAUD, DEFAULT_DATA_BITS, DEFAULT_PARITY, DEFAULT_STOP_BITS);
	}

	public SerialDevice(String deviceName, int baud, DataBits dataBits, Parity parity, StopBits stopBits) {
		device = DeviceFactoryHelper.getNativeDeviceFactory().provisionSerialDevice(deviceName, baud, dataBits, parity,
				stopBits);
	}

	@Override
	public void close() throws IOException {
		Logger.trace("close()");
		device.close();
	}
	
	public int read() {
		return device.read();
	}
	
	public byte readByte() {
		return device.readByte();
	}
	
	public void readByte(byte bVal) {
		device.writeByte(bVal);
	}
	
	public void read(byte[] buffer) {
		device.read(buffer);
	}
	
	public void write(byte[] buffer) {
		device.write(buffer);
	}
	
	public int bytesAvailable() {
		return device.bytesAvailable();
	}
}
