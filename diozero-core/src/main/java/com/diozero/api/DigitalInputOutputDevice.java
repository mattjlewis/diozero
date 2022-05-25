package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DigitalInputOutputDevice.java
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

import org.tinylog.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * A digital device whose {@link DeviceMode Mode} can be initialised to and
 * changed to {@link DeviceMode DIGITAL_INPUT} and {@link DeviceMode
 * DIGITAL_OUTPUT}.
 */
public class DigitalInputOutputDevice extends AbstractDigitalInputDevice {
	private GpioDigitalInputOutputDeviceInterface delegate;
	private DeviceMode mode;

	/**
	 * @param gpio GPIO to which the device is connected.
	 * @param mode Input or output {@link DeviceMode Mode}
	 * @throws RuntimeIOException If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(int gpio, DeviceMode mode) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, mode);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this digital input
	 *                      device.
	 * @param gpio          GPIO to which the device is connected.
	 * @param mode          Input or output {@link DeviceMode Mode}
	 * @throws RuntimeIOException If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(GpioDeviceFactoryInterface deviceFactory, int gpio, DeviceMode mode)
			throws RuntimeIOException {
		this(deviceFactory, deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio), mode);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this digital input
	 *                      device.
	 * @param pinInfo       Information about the GPIO pin to which the device is
	 *                      connected.
	 * @param mode          Input or output {@link DeviceMode Mode}
	 * @throws RuntimeIOException If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(GpioDeviceFactoryInterface deviceFactory, PinInfo pinInfo, DeviceMode mode)
			throws RuntimeIOException {
		super(pinInfo, false);

		this.delegate = deviceFactory.provisionDigitalInputOutputDevice(pinInfo, mode);
		this.mode = mode;
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		if (delegate.isOpen()) {
			removeAllListeners();
			delegate.close();
		}
	}

	/**
	 * Get the input / output mode
	 *
	 * @return current mode
	 */
	public DeviceMode getMode() {
		return mode;
	}

	/**
	 * Set the input / output mode
	 *
	 * @param mode new mode, valid values are {@link DeviceMode DIGITAL_INPUT} and
	 *             {@link DeviceMode DIGITAL_OUTPUT}
	 */
	public void setMode(DeviceMode mode) {
		if (mode == this.mode) {
			return;
		}
		if (mode != DeviceMode.DIGITAL_INPUT && mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new InvalidModeException("Invalid mode value, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
		}

		delegate.setMode(mode);
		this.mode = mode;
	}

	/**
	 * Read the current underlying state of the input pin. Does not factor in active
	 * high logic.
	 *
	 * @return Device state.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	@Override
	public boolean getValue() throws RuntimeIOException {
		return delegate.getValue();
	}

	/**
	 * Set the output value (if in output mode).
	 *
	 * @param value The new value
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		delegate.setValue(value);
	}

	@Override
	protected void setListener() {
		delegate.setListener(this);
	}

	@Override
	protected void removeListener() {
		delegate.removeListener();
	}
}
