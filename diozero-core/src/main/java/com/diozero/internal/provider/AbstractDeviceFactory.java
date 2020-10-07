package com.diozero.internal.provider;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     AbstractDeviceFactory.java  
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

import com.diozero.api.PinInfo;
import com.diozero.internal.DeviceStates;
import com.diozero.util.DeviceFactoryHelper;

public abstract class AbstractDeviceFactory implements DeviceFactoryInterface {
	private String deviceFactoryPrefix;
	protected DeviceStates deviceStates;
	protected boolean closed;

	public AbstractDeviceFactory(String deviceFactoryPrefix) {
		this.deviceFactoryPrefix = deviceFactoryPrefix;
		deviceStates = new DeviceStates();

		// Register all expansion boards so that they can be cleaned up on shutdown
		if (!(this instanceof BaseNativeDeviceFactory)) {
			DeviceFactoryHelper.getNativeDeviceFactory().registerDeviceFactory(this);
		}
	}

	@Override
	public final String createPinKey(PinInfo pinInfo) {
		return deviceFactoryPrefix + "-" + pinInfo.getKeyPrefix() + "-" + pinInfo.getDeviceNumber();
	}

	@Override
	public final String createI2CKey(int controller, int address) {
		return I2CDeviceFactoryInterface.createI2CKey(deviceFactoryPrefix, controller, address);
	}

	@Override
	public final String createSpiKey(int controller, int chipSelect) {
		return SpiDeviceFactoryInterface.createSpiKey(deviceFactoryPrefix, controller, chipSelect);
	}

	@Override
	public final String createSerialKey(String tty) {
		return SerialDeviceFactoryInterface.createSerialKey(deviceFactoryPrefix, tty);
	}

	@Override
	public void close() {
		if (!closed) {
			Logger.trace("close()");
			deviceStates.closeAll();
			closed = true;
		}
	}

	@Override
	public final boolean isClosed() {
		return closed;
	}

	@Override
	public final void deviceOpened(DeviceInterface device) {
		deviceStates.opened(device);
	}

	@Override
	public final void deviceClosed(DeviceInterface device) {
		deviceStates.closed(device);
	}

	@Override
	public final boolean isDeviceOpened(String key) {
		return deviceStates.isOpened(key);
	}

	@Override
	public final DeviceInterface getDevice(String key) {
		return deviceStates.getDevice(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T extends DeviceInterface> T getDevice(String key, Class<T> deviceClass) {
		return (T) deviceStates.getDevice(key);
	}
}
