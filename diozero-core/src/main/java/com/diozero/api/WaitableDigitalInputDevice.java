package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     WaitableDigitalInputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * Represents a digital input device with distinct waitable states (active /
 * inactive).
 * @deprecated Functionality has moved up to AbstractDigitalInputDevice
 */
@Deprecated
public class WaitableDigitalInputDevice extends DigitalInputDevice {
	/**
	 * @param gpio GPIO to which the device is connected.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public WaitableDigitalInputDevice(int gpio) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	/**
	 * @param gpio    GPIO to which the device is connected.
	 * @param pud     Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
	 * @param trigger Event trigger configuration, values: NONE, RISING, FALLING,
	 *                BOTH.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public WaitableDigitalInputDevice(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger)
			throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pud, trigger);
	}

	/**
	 * @param deviceFactory Device factory to use to construct the device.
	 * @param gpio          GPIO to which the device is connected.
	 * @param pud           Pull up/down configuration, values: NONE, PULL_UP,
	 *                      PULL_DOWN.
	 * @param trigger       Event trigger configuration, values: NONE, RISING,
	 *                      FALLING, BOTH.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public WaitableDigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		super(deviceFactory, gpio, pud, trigger);
		enableDeviceListener();
	}

	@Override
	protected void disableDeviceListener() {
		// Never disable the device listener
	}
}
