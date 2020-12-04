package com.diozero.api;

import com.diozero.api.function.Action;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     AbstractDigitalInputDevice.java  
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

/**
 * Abstract base class for low-level GPIO digital input devices.
 */
public abstract class AbstractDigitalInputDevice extends GpioInputDevice<DigitalInputEvent>
		implements DigitalInputDeviceInterface {
	protected boolean activeHigh;
	private Action activatedAction;
	private Action deactivatedAction;
	private boolean listenerEnabled;

	public AbstractDigitalInputDevice(int gpio, boolean activeHigh) {
		super(gpio);

		this.activeHigh = activeHigh;
	}

	/**
	 * Get active high configuration.
	 * 
	 * @return Returns false if configured as pull-up, true for all other pull up /
	 *         down options.
	 */
	public boolean isActiveHigh() {
		return activeHigh;
	}

	@Override
	protected void enableDeviceListener() {
		if (!listenerEnabled) {
			setListener();
			listenerEnabled = true;
		}
	}

	@Override
	protected void disableDeviceListener() {
		if (activatedAction == null && deactivatedAction == null) {
			if (listenerEnabled) {
				removeListener();
				listenerEnabled = false;
			}
		}
	}

	@Override
	public void accept(DigitalInputEvent event) {
		event.setActiveHigh(activeHigh);
		if (activatedAction != null && event.isActive()) {
			activatedAction.action();
		}
		if (deactivatedAction != null && !event.isActive()) {
			deactivatedAction.action();
		}
		super.accept(event);
	}

	/**
	 * Action to perform when the device state is active.
	 * 
	 * @param action Action callback object.
	 */
	public void whenActivated(Action action) {
		activatedAction = action;
		if (activatedAction == null && deactivatedAction == null) {
			disableDeviceListener();
		} else {
			enableDeviceListener();
		}
	}

	/**
	 * Action to perform when the device state is inactive.
	 * 
	 * @param action Action callback object.
	 */
	public void whenDeactivated(Action action) {
		deactivatedAction = action;
		if (activatedAction == null && deactivatedAction == null) {
			disableDeviceListener();
		} else {
			enableDeviceListener();
		}
	}

	protected abstract void setListener();

	protected abstract void removeListener();
}
