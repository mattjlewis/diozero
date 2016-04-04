package com.diozero.api;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.Event;

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

import com.diozero.util.RuntimeIOException;

/**
 * Represents a digital input device with distinct waitable states.
 * This class extends 'DigitalInputDevice' with methods for waiting on the device's
 * status ('waitForActive' and 'waitForInactive').
 */
public class WaitableDigitalInputDevice extends DigitalInputDevice {
	protected DigitalInputEvent lastPinEvent;
	private Event highEvent = new Event();
	private Event lowEvent = new Event();

	public WaitableDigitalInputDevice(int pinNumber) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	public WaitableDigitalInputDevice(int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, pud, trigger);
	}

	public WaitableDigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber,
			GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		super(deviceFactory, pinNumber, pud, trigger);
		enableListener();
	}
	
	@Override
	protected void disableListener() {
	}

	@Override
	public void valueChanged(DigitalInputEvent event) {
		Event e = event.getValue() ? highEvent : lowEvent;
		e.set();
		
		// Notify any listeners
		super.valueChanged(event);
	}

	/**
	 * Wait indefinitely for the device state to go active.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.-
	 */
	public boolean waitForActive() throws InterruptedException {
		return waitForActive(0);
	}

	/**
	 * Wait the specified time period for the device state to go active.
	 * @param timeout Timeout value if milliseconds, &lt;= 0 is indefinite.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.-
	 */
	public boolean waitForActive(int timeout) throws InterruptedException {
		return waitForValue(activeHigh, timeout);
	}

	/**
	 * Wait indefinitely for the device state to go inactive.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.-
	 */
	public boolean waitForInactive() throws InterruptedException {
		return waitForInactive(0);
	}

	/**
	 * Wait the specified time period for the device state to go inactive.
	 * @param timeout Timeout value if milliseconds, &lt;= 0 is indefinite.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.-
	 */
	public boolean waitForInactive(int timeout) throws InterruptedException {
		return waitForValue(!activeHigh, timeout);
	}

	/**
	 * Wait the specified time period for the device state to switch to value.
	 * @param value The desired device state to wait for.
	 * @param timeout Timeout value if milliseconds, &lt;= 0 is indefinite.
	 * @return False if timed out waiting for the specified value, otherwise true.
	 * @throws InterruptedException If interrupted while waiting.-
	 */
	public boolean waitForValue(boolean value, int timeout) throws InterruptedException {
		Event e = value ? highEvent : lowEvent;
		if (timeout > 0) {
			return e.doWait(timeout);
		}

		return e.doWait();
	}
}
