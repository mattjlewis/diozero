package com.diozero.internal;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DeviceStates.java
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.internal.spi.InternalDeviceInterface;

public class DeviceStates {
	private Map<String, InternalDeviceInterface> devices;

	public DeviceStates() {
		devices = new ConcurrentHashMap<>();
	}

	public boolean isOpened(String key) {
		return devices.containsKey(key);
	}

	public void opened(InternalDeviceInterface device) {
		devices.put(device.getKey(), device);
	}

	public void closed(InternalDeviceInterface device) {
		Logger.trace("closed({})", device.getKey());
		if (devices.remove(device.getKey()) == null) {
			Logger.warn("Request to close unknown device with key '{}'", device.getKey());
		}
	}

	public void closeAll() {
		Logger.trace("closeAll()");
		// collect all the things
		List<InternalDeviceInterface> closeThese = devices.values().stream()
				.filter(device -> !device.isChild()).collect(Collectors.toList());

		// calling "close" on each should end up calling closed (above), which should remove everything from the map
		closeThese.forEach(InternalDeviceInterface::close);
	}

	public InternalDeviceInterface getDevice(String key) {
		return devices.get(key);
	}

	public int size() {
		return devices.size();
	}
}
