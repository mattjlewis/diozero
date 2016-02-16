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

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Represents a generic input device.
 * 
 */
public class DigitalInputDevice extends GpioInputDevice<DigitalPinEvent> {
	protected boolean activeHigh;
	protected GpioDigitalInputDeviceInterface device;
	protected GpioPullUpDown pud;

	public DigitalInputDevice(int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, pud, trigger);
	}

	public DigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		super(pinNumber);
		
		this.device = deviceFactory.provisionDigitalInputPin(pinNumber, pud, trigger);
		this.pud = pud;
		this.activeHigh = pud != GpioPullUpDown.PULL_UP;
	}

	@Override
	public void close() {
		Logger.debug("close()");
		device.close();
	}

	public boolean getValue() throws RuntimeIOException {
		return device.getValue();
	}
	
	public boolean isActive() throws RuntimeIOException {
		return device.getValue() == activeHigh;
	}

	@Override
	public void enableListener() {
		device.setListener(this);
	}

	@Override
	public void disableListener() {
		device.removeListener();
	}
}
