package com.diozero.internal.provider;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     I2CDeviceInterface.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;

public interface I2CDeviceInterface extends DeviceInterface {
	boolean probe(I2CDevice.ProbeMode mode) throws RuntimeIOException;
	
	byte readByte() throws RuntimeException;
	void writeByte(byte b) throws RuntimeException;
	
	void read(ByteBuffer buffer) throws RuntimeException;
	void write(ByteBuffer buffer) throws RuntimeException;
	
	byte readByteData(int register) throws RuntimeIOException;
	void writeByteData(int register, byte b) throws RuntimeIOException;
	
	void readI2CBlockData(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException;
	void writeI2CBlockData(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException;
}
