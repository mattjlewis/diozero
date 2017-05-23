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

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.provider.GpioDeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Represents a generic digital input device.
 */
public class DigitalInputDevice extends GpioInputDevice<DigitalInputEvent>
implements DigitalInputDeviceInterface {
	protected boolean activeHigh;
	protected GpioDigitalInputDeviceInterface device;
	private GpioPullUpDown pud;
	private GpioEventTrigger trigger;
	private Action activatedAction;
	private Action deactivatedAction;
	private boolean listenerEnabled;

	/**
	 * @param gpio
	 *            GPIO to which the device is connected.
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputDevice(int gpio) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	/**
	 * @param gpio
	 *            GPIO to which the device is connected.
	 * @param pud
	 *            Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
	 * @param trigger
	 *            Event trigger configuration, values: NONE, RISING, FALLING,
	 *            BOTH.
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputDevice(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pud, trigger);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to provision this digital input device.
	 * @param gpio
	 *            GPIO to which the device is connected.
	 * @param pud
	 *            Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
	 * @param trigger
	 *            Event trigger configuration, values: NONE, RISING, FALLING,
	 *            BOTH.
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		super(gpio);

		this.device = deviceFactory.provisionDigitalInputDevice(gpio, pud, trigger);
		this.pud = pud;
		this.trigger = trigger;
		this.activeHigh = pud != GpioPullUpDown.PULL_UP;
	}

	@Override
	public void close() {
		Logger.debug("close()");
		device.close();
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
		synchronized (device) {
			if (! listenerEnabled) {
				device.setListener(this);
				listenerEnabled = true;
			}
		}
	}

	@Override
	protected void disableListener() {
		if (listeners.isEmpty() && activatedAction == null && deactivatedAction == null) {
			synchronized (device) {
				if (listenerEnabled) {
					device.removeListener();
					listenerEnabled = false;
				}
			}
		}
	}

	/**
	 * Get pull up / down configuration.
	 * 
	 * @return Pull up / down configuration.
	 */
	public GpioPullUpDown getPullUpDown() {
		return pud;
	}

	/**
	 * Get event trigger configuration.
	 * 
	 * @return Event trigger configuration.
	 */
	public GpioEventTrigger getTrigger() {
		return trigger;
	}

	/**
	 * Get active high configuration.
	 * 
	 * @return Returns false if configured as pull-up, true for all other pull
	 *         up / down options.
	 */
	public boolean isActiveHigh() {
		return activeHigh;
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
	 * Read the current on/off state for this device taking into account the
	 * pull up / down configuration. If the input is pulled up
	 * {@code isActive()} will return {@code true} when when the value is
	 * {@code false}.
	 * 
	 * @return Device active state.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public boolean isActive() throws RuntimeIOException {
		return device.getValue() == activeHigh;
	}

	/**
	 * Action to perform when the device state is active.
	 * 
	 * @param action
	 *            Action callback object.
	 */
	public void whenActivated(Action action) {
		activatedAction = action;
		if (action == null) {
			disableListener();
		} else {
			enableListener();
		}
	}

	/**
	 * Action to perform when the device state is inactive.
	 * 
	 * @param action
	 *            Action callback object.
	 */
	public void whenDeactivated(Action action) {
		deactivatedAction = action;
		if (action == null) {
			disableListener();
		} else {
			enableListener();
		}
	}
}
