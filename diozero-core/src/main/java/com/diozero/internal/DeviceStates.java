package com.diozero.internal;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     DeviceStates.java  
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.DeviceInterface;

public class DeviceStates {
	private Map<String, DeviceInterface> devices;
	
	public DeviceStates() {
		devices = new ConcurrentHashMap<>();
	}

	public boolean isOpened(String key) {
		return devices.containsKey(key);
	}
	
	public void opened(DeviceInterface device) {
		devices.put(device.getKey(), device);
	}
	
	public void closed(DeviceInterface device) {
		Logger.debug("closed({})", device.getKey());
		if (devices.remove(device.getKey()) == null) {
			Logger.warn("Request to close unknown device with key '{}'", device.getKey());
		}
	}
	
	public void closeAll() {
		Logger.debug("closeAll()");
		// No need to remove from the Map as close() should always call closed()
		devices.values().forEach(DeviceInterface::close);
	}

	public DeviceInterface getDevice(String key) {
		return devices.get(key);
	}
	
	public int size() {
		return devices.size();
	}
}
