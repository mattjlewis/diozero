package com.diozero.internal.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     DeviceFactoryInterface.java  
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

import com.diozero.api.DeviceInterface;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.sbc.BoardPinInfo;

public interface DeviceFactoryInterface extends Closeable {
	default void start() {
		// Do nothing
	}
	
	String getName();
	boolean isDeviceOpened(String key);
	void deviceOpened(DeviceInterface device);
	void deviceClosed(DeviceInterface device);
	BoardPinInfo getBoardPinInfo();
	String createPinKey(PinInfo pinInfo);
	String createI2CKey(int controller, int address);
	String createSpiKey(int controller, int chipSelect);
	String createSerialKey(String deviceFile);
	@Override
	void close() throws RuntimeIOException;
	void reopen();
	
	boolean isClosed();
	DeviceInterface getDevice(String key);
	<T extends DeviceInterface> T getDevice(String key, Class<T> deviceClass);
}
