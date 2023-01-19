package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AbstractDigitalInputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import java.util.function.LongConsumer;

import com.diozero.util.EventLock;

/**
 * Abstract base class for low-level GPIO digital input devices.
 */
public abstract class AbstractDigitalInputDevice extends GpioInputDevice<DigitalInputEvent>
		implements DigitalInputDeviceInterface {
	protected boolean activeHigh;
	private LongConsumer activatedConsumer;
	private LongConsumer deactivatedConsumer;
	private boolean listenerEnabled;
	private EventLock highEvent;
	private EventLock lowEvent;

	public AbstractDigitalInputDevice(PinInfo pinInfo, boolean activeHigh) {
		super(pinInfo);

		this.activeHigh = activeHigh;
		highEvent = new EventLock();
		lowEvent = new EventLock();
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
		// Ignore if there is an activated / deactivated consumer or listeners
		if (activatedConsumer == null && deactivatedConsumer == null && !hasListeners()) {
			if (listenerEnabled) {
				removeListener();
				listenerEnabled = false;
			}
		}
	}

	@Override
	public void accept(DigitalInputEvent event) {
		event.setActiveHigh(activeHigh);

		EventLock e = event.getValue() ? highEvent : lowEvent;
		e.set();

		if (activatedConsumer != null && event.isActive()) {
			activatedConsumer.accept(event.getNanoTime());
		}

		if (deactivatedConsumer != null && !event.isActive()) {
			deactivatedConsumer.accept(event.getNanoTime());
		}

		super.accept(event);
	}

	/**
	 * Action to perform when the device state is active.
	 *
	 * @param consumer Callback object to be invoked when activated (long parameter
	 *                 is nanoseconds time).
	 */
	public void whenActivated(LongConsumer consumer) {
		activatedConsumer = consumer;
		if (consumer == null) {
			disableDeviceListener();
		} else {
			enableDeviceListener();
		}
	}

	/**
	 * Action to perform when the device state is inactive.
	 *
	 * @param consumer Callback object to be invoked when activated (long parameter
	 *                 is nanoseconds time)
	 */
	public void whenDeactivated(LongConsumer consumer) {
		deactivatedConsumer = consumer;
		if (consumer != null) {
			disableDeviceListener();
		} else {
			enableDeviceListener();
		}
	}

	/**
	 * Wait indefinitely for the device state to go active.
	 *
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	public boolean waitForActive() throws InterruptedException {
		return waitForActive(0);
	}

	/**
	 * Wait the specified time period for the device state to go active.
	 *
	 * @param timeout Timeout value if milliseconds, &lt;= 0 is indefinite.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	public boolean waitForActive(int timeout) throws InterruptedException {
		return waitForValue(activeHigh, timeout);
	}

	/**
	 * Wait indefinitely for the device state to go inactive.
	 *
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	public boolean waitForInactive() throws InterruptedException {
		return waitForInactive(0);
	}

	/**
	 * Wait the specified time period for the device state to go inactive.
	 *
	 * @param timeout Timeout value if milliseconds, &lt;= 0 is indefinite.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	public boolean waitForInactive(int timeout) throws InterruptedException {
		return waitForValue(!activeHigh, timeout);
	}

	/**
	 * Wait the specified time period for the device state to switch to the
	 * specified value, not taking into account active high / low logic.
	 *
	 * @param value   The desired device state to wait for.
	 * @param timeout Timeout value if milliseconds, &lt;= 0 is indefinite.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	public boolean waitForValue(boolean value, int timeout) throws InterruptedException {
		enableDeviceListener();

		EventLock e = value ? highEvent : lowEvent;
		if (timeout > 0) {
			return e.doWait(timeout);
		}

		return e.doWait();
	}

	protected abstract void setListener();

	protected abstract void removeListener();
}
