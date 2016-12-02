package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import org.pmw.tinylog.Logger;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class DigitalInputOutputDevice extends GpioDevice
implements DigitalInputDeviceInterface {
	protected GpioDigitalInputOutputDeviceInterface device;
	private GpioDeviceInterface.Mode mode;

	/**
	 * @param pinNumber
	 *            Pin number to which the device is connected.
	 * @param mode
	 *            Input or output {@link com.diozero.internal.spi.GpioDeviceInterface.Mode Mode}
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(int pinNumber, GpioDeviceInterface.Mode mode) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, mode);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to provision this digital input device.
	 * @param pinNumber
	 *            Pin number to which the device is connected.
	 * @param mode
	 *            Input or output {@link com.diozero.internal.spi.GpioDeviceInterface.Mode Mode}
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber,
			GpioDeviceInterface.Mode mode) throws RuntimeIOException {
		super(pinNumber);
		
		checkMode(mode);
		
		this.device = deviceFactory.provisionDigitalInputOutputPin(pinNumber, mode);
		this.mode = mode;
	}
	
	private static void checkMode(GpioDeviceInterface.Mode mode) {
		if (mode != GpioDeviceInterface.Mode.DIGITAL_INPUT && mode != GpioDeviceInterface.Mode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Invalid mode value, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
		}
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		device.close();
	}
	
	/**
	 * Get the input / output mode
	 * @return current mode
	 */
	public GpioDeviceInterface.Mode getMode() {
		return mode;
	}
	
	/**
	 * Set the input / output mode
	 * @param mode new mode, valid values are {@link com.diozero.internal.spi.GpioDeviceInterface.Mode DIGITAL_INPUT} and {@link com.diozero.internal.spi.GpioDeviceInterface.Mode DIGITAL_OUTPUT}
	 */
	public void setMode(GpioDeviceInterface.Mode mode) {
		if (mode == this.mode) {
			return;
		}
		checkMode(mode);
		
		device.setMode(mode);
		this.mode = mode;
	}

	/**
	 * Read the current underlying state of the input pin. Does not factor in
	 * active high logic.
	 * 
	 * @return Device state.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	@Override
	public boolean getValue() throws RuntimeIOException {
		return device.getValue();
	}

	/**
	 * Set the output value (if mode.
	 * 
	 * @param value
	 *            The new value
	 * @throws RuntimeIOException
	 *             If an I/O error occurs
	 */
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != GpioDeviceInterface.Mode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		device.setValue(value);
	}
}
