package com.diozero.api;

import java.io.IOException;

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
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

/**
 * Represents a generic input device.
 * 
 */
public class DigitalInputDevice extends GpioInputDevice<DigitalInputEvent> {
	protected boolean activeHigh;
	protected GpioDigitalInputDeviceInterface device;
	protected GpioPullUpDown pud;
	protected GpioEventTrigger trigger;
	private Action activatedAction;
	private Action deactivatedAction;

	public DigitalInputDevice(int pinNumber) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	public DigitalInputDevice(int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, pud, trigger);
	}

	public DigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		super(pinNumber);
		
		this.device = deviceFactory.provisionDigitalInputPin(pinNumber, pud, trigger);
		this.pud = pud;
		this.trigger = trigger;
		this.activeHigh = pud != GpioPullUpDown.PULL_UP;
	}

	@Override
	public void close() {
		Logger.debug("close()");
		try {
			device.close();
		} catch (IOException e) {
			// Log and ignore
			Logger.warn(e, "Error closing device: {}", e);
		}
	}
	
	@Override
	public void valueChanged(DigitalInputEvent event) {
		event.setActiveHigh(activeHigh);
		if (activatedAction != null && event.isActive()) {
			activatedAction.action();
		}
		if (deactivatedAction != null && !event.isActive()) {
			deactivatedAction.action();
		}
		super.valueChanged(event);
	}

	@Override
	protected void enableListener() {
		device.setListener(this);
	}

	@Override
	protected void disableListener() {
		if (activatedAction == null && deactivatedAction == null) {
			device.removeListener();
		}
	}
	
	public GpioPullUpDown getPullUpDown() {
		return pud;
	}

	public GpioEventTrigger getTrigger() {
		return trigger;
	}
	
	public boolean isActiveHigh() {
		return activeHigh;
	}
	
	public boolean getValue() throws RuntimeIOException {
		return device.getValue();
	}
	
	public boolean isActive() throws RuntimeIOException {
		return device.getValue() == activeHigh;
	}
	
	public void whenActivated(Action action) {
		activatedAction = action;
		if (action != null) {
			enableListener();
		} else if (listeners.isEmpty() && deactivatedAction == null) {
			disableListener();
		}
	}
	
	public void whenDeactivated(Action action) {
		deactivatedAction = action;
		if (action != null) {
			enableListener();
		} else if (listeners.isEmpty() && activatedAction == null) {
			disableListener();
		}
	}
}
