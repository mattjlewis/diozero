package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     GpioInputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import java.util.ArrayList;
import java.util.Collection;

import com.diozero.api.function.DeviceEventConsumer;

/**
 * Common base class for digital and analog input devices.
 *
 * @param <T> Event class that extends DeviceEvent. See
 *            {@link com.diozero.api.DigitalInputEvent DigitalInputEvent} and
 *            {@link com.diozero.api.AnalogInputEvent AnalogInputEvent}.
 */
public abstract class GpioInputDevice<T extends DeviceEvent> extends GpioDevice implements DeviceEventConsumer<T> {
	private Collection<DeviceEventConsumer<T>> listeners;

	/**
	 * @param pinInfo PinInfo object for the GPIO
	 */
	public GpioInputDevice(PinInfo pinInfo) {
		super(pinInfo);
		listeners = new ArrayList<>();
	}

	/**
	 * Add a new listener
	 *
	 * @param listener Callback instance
	 */
	public void addListener(DeviceEventConsumer<T> listener) {
		if (listeners.isEmpty()) {
			enableDeviceListener();
		}
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a specific listener
	 *
	 * @param listener Callback instance to remove
	 */
	public void removeListener(DeviceEventConsumer<T> listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			disableDeviceListener();
		}
	}

	/**
	 * Remove all listeners
	 */
	public void removeAllListeners() {
		listeners.clear();
		disableDeviceListener();
	}

	public boolean hasListeners() {
		return !listeners.isEmpty();
	}

	@Override
	public void accept(T event) {
		listeners.forEach(listener -> listener.accept(event));
	}

	protected abstract void enableDeviceListener();

	protected abstract void disableDeviceListener();
}
