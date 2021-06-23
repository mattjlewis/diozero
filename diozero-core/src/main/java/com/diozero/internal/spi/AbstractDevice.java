package com.diozero.internal.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AbstractDevice.java
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

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;

public abstract class AbstractDevice implements InternalDeviceInterface {
	private String key;
	private DeviceFactoryInterface deviceFactory;
	private boolean child;

	public AbstractDevice(String key, DeviceFactoryInterface deviceFactory) {
		this.key = key;
		this.deviceFactory = deviceFactory;
	}

	@Override
	public final String getKey() {
		return key;
	}

	@Override
	public boolean isOpen() {
		return deviceFactory.isDeviceOpened(key);
	}

	@Override
	public boolean isChild() {
		return child;
	}

	@Override
	public void setChild(boolean child) {
		this.child = child;
	}

	@Override
	public final void close() {
		Logger.trace("close(), key={}", key);
		if (!isOpen()) {
			Logger.warn("Device '{}' already closed", key);
			return;
		}

		try {
			closeDevice();
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error closing device {}: {}", key, e);
		}
		deviceFactory.deviceClosed(this);
	}

	protected DeviceFactoryInterface getDeviceFactory() {
		return deviceFactory;
	}

	protected abstract void closeDevice() throws RuntimeIOException;
}
