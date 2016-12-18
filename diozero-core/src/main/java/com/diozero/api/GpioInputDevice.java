package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

/**
 * Common base class for digital and analog input devices.
 *
 * @param <T>
 *            Event class that extends DeviceEvent. See
 *            {@link com.diozero.api.DigitalInputEvent DigitalInputEvent} and
 *            {@link com.diozero.api.AnalogInputEvent AnalogInputEvent}.
 */
public abstract class GpioInputDevice<T extends DeviceEvent> extends GpioDevice
implements InputEventListener<T> {
	protected Collection<InputEventListener<T>> listeners;

	/**
	 * @param gpio
	 *            GPIO to which the device is connected.
	 */
	public GpioInputDevice(int gpio) {
		super(gpio);
		listeners = new ArrayList<>();
	}

	/**
	 * Add a new listener.
	 * 
	 * @param listener
	 *            Callback instance.
	 */
	public void addListener(InputEventListener<T> listener) {
		if (listeners.isEmpty()) {
			enableListener();
		}
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a specific listener.
	 * 
	 * @param listener
	 *            Callback instance to remove.
	 */
	public void removeListener(InputEventListener<T> listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			disableListener();
		}
	}

	/**
	 * Remove all listeners.
	 */
	public void removeAllListeners() {
		listeners.clear();
	}

	@Override
	public void valueChanged(T event) {
		listeners.forEach(listener -> listener.valueChanged(event));
	}

	protected abstract void enableListener();

	protected abstract void disableListener();
}
