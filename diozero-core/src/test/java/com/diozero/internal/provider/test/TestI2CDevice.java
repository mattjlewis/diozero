package com.diozero.internal.provider.test;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     TestI2CDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class TestI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	public TestI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int address, int addressSize, int clockFrequency) {
		super(key, deviceFactory);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
	}
	
	@Override
	public boolean probe(I2CDevice.ProbeMode mode) {
		return true;
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void read(ByteBuffer buffer) throws RuntimeIOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void write(ByteBuffer buffer) throws RuntimeIOException {
		// TODO Auto-generated method stub
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void readI2CBlockData(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void writeI2CBlockData(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		// TODO Auto-generated method stub
	}
}
