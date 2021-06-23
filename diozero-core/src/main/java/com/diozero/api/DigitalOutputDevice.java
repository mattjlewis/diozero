/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DigitalOutputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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

package com.diozero.api;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.api.function.Action;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * Provides generic digital (on/off) output control with support for active high
 * and low logic.
 */
public class DigitalOutputDevice extends GpioDevice {
	public static final class Builder {
		public static Builder builder(int gpio) {
			return new Builder(gpio);
		}

		public static Builder builder(PinInfo pinInfo) {
			return new Builder(pinInfo);
		}

		private Integer gpio;
		private PinInfo pinInfo;
		private boolean activeHigh = true;
		private boolean initialValue = false;
		private GpioDeviceFactoryInterface deviceFactory;

		public Builder(int gpio) {
			this.gpio = Integer.valueOf(gpio);
		}

		public Builder(PinInfo pinInfo) {
			this.pinInfo = pinInfo;
		}

		public Builder setActiveHigh(boolean activeHigh) {
			this.activeHigh = activeHigh;
			return this;
		}

		public Builder setInitialValue(boolean initialValue) {
			this.initialValue = initialValue;
			return this;
		}

		public Builder setDeviceFactory(GpioDeviceFactoryInterface deviceFactory) {
			this.deviceFactory = deviceFactory;
			return this;
		}

		public DigitalOutputDevice build() {
			// Default to the native device factory if not set
			if (deviceFactory == null) {
				deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
			}

			if (pinInfo == null) {
				pinInfo = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio.intValue());
			}

			return new DigitalOutputDevice(deviceFactory, pinInfo, activeHigh, initialValue);
		}
	}

	public static final int INFINITE_ITERATIONS = -1;

	private boolean activeHigh;
	private AtomicBoolean running;
	private Future<?> future;
	private GpioDigitalOutputDeviceInterface delegate;
	private int cycleCount;

	/**
	 * Defaults to active high logic, initial value is off.
	 *
	 * @param gpio GPIO to which the output device is connected.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public DigitalOutputDevice(int gpio) throws RuntimeIOException {
		this(gpio, true, false);
	}

	/**
	 * @param gpio         GPIO to which the output device is connected.
	 * @param activeHigh   If true then setting the value to true will turn on the
	 *                     connected device.
	 * @param initialValue Initial output value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public DigitalOutputDevice(int gpio, boolean activeHigh, boolean initialValue) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, activeHigh, initialValue);
	}

	/**
	 * @param deviceFactory Device factory to use to construct the device.
	 * @param gpio          GPIO to which the output device is connected.
	 * @param activeHigh    If true then setting the value to true will turn on the
	 *                      connected device.
	 * @param initialValue  Initial output value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public DigitalOutputDevice(GpioDeviceFactoryInterface deviceFactory, int gpio, boolean activeHigh,
			boolean initialValue) throws RuntimeIOException {
		this(deviceFactory, deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio), activeHigh, initialValue);
	}

	/**
	 * @param deviceFactory Device factory to use to construct the device.
	 * @param pinInfo       Information about the GPIO pin to which the output
	 *                      device is connected.
	 * @param activeHigh    If true then setting the value to true will turn on the
	 *                      connected device.
	 * @param initialValue  Initial output value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public DigitalOutputDevice(GpioDeviceFactoryInterface deviceFactory, PinInfo pinInfo, boolean activeHigh,
			boolean initialValue) throws RuntimeIOException {
		super(pinInfo);

		this.delegate = deviceFactory.provisionDigitalOutputDevice(pinInfo, activeHigh == initialValue);
		this.activeHigh = activeHigh;
		running = new AtomicBoolean();
	}

	@Override
	public void close() {
		Logger.trace("close()");
		stopOnOffLoop();
		if (delegate.isOpen()) {
			setOn(false);
			delegate.close();
		}
	}

	private void onOffLoop(int onTimeMs, int offTimeMs, int n, Action stopAction) throws RuntimeIOException {
		running.set(true);
		cycleCount = 0;
		if (n > 0) {
			for (int i = 0; i < n && running.get(); i++) {
				onOff(onTimeMs, offTimeMs);
			}
			running.set(false);
		} else if (n == INFINITE_ITERATIONS) {
			while (running.get()) {
				onOff(onTimeMs, offTimeMs);
			}
		}
		if (stopAction != null) {
			stopAction.action();
		}
	}

	private void onOff(int onTimeMs, int offTimeMs) throws RuntimeIOException {
		setValue(activeHigh);
		try {
			Thread.sleep(onTimeMs);
		} catch (InterruptedException e) {
			running.set(false);
		}

		setValue(!activeHigh);
		cycleCount++;

		if (!running.get()) {
			return;
		}

		try {
			Thread.sleep(offTimeMs);
		} catch (InterruptedException e) {
			running.set(false);
		}
	}

	public void stopOnOffLoop() {
		running.set(false);
		if (future != null) {
			future.cancel(true);
			try {
				future.get();
			} catch (Exception e) {
				// Ignore
			}
			future = null;
		}
	}

	// Exposed operations

	/**
	 * Turn on the device compensating for active low/high logic levels. Note that
	 * this method does not check if the on-off loop is running.
	 *
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void on() throws RuntimeIOException {
		setValue(activeHigh);
	}

	/**
	 * Turn off the device compensating for active low/high logic levels. Note that
	 * this method does not check if the on-off loop is running.
	 *
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void off() throws RuntimeIOException {
		setValue(!activeHigh);
	}

	/**
	 * Toggle the state of the device.
	 *
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void toggle() throws RuntimeIOException {
		setValue(!delegate.getValue());
	}

	/**
	 * Get the device on / off status.
	 *
	 * @return Returns true if the device is currently on.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public boolean isOn() throws RuntimeIOException {
		return activeHigh == delegate.getValue();
	}

	/**
	 * Turn the device on or off.
	 *
	 * @param on New on/off value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void setOn(boolean on) throws RuntimeIOException {
		setValue(activeHigh & on);
	}

	/**
	 * Set the device output value without compensating for active low/high logic
	 * levels. Note that this method does not check if the on-off loop is running.
	 *
	 * @param value The new value
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public void setValue(boolean value) throws RuntimeIOException {
		delegate.setValue(value);
	}

	/**
	 * Toggle the device on-off.
	 *
	 * @param onTime     On time in seconds.
	 * @param offTime    Off time in seconds.
	 * @param n          Number of iterations. Set to &lt;0 to blink indefinitely.
	 * @param background If true start a background thread to control the blink and
	 *                   return immediately. If false, only return once the blink
	 *                   iterations have finished.
	 * @param stopAction Action to perform when the loop finishes
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public void onOffLoop(float onTime, float offTime, int n, boolean background, Action stopAction)
			throws RuntimeIOException {
		stopOnOffLoop();
		int on_ms = (int) (onTime * SleepUtil.MS_IN_SEC);
		int off_ms = (int) (offTime * SleepUtil.MS_IN_SEC);
		if (background) {
			future = DiozeroScheduler.getNonDaemonInstance().submit(() -> onOffLoop(on_ms, off_ms, n, stopAction));
		} else {
			onOffLoop(on_ms, off_ms, n, stopAction);
		}
	}

	public boolean isActiveHigh() {
		return activeHigh;
	}

	public int getCycleCount() {
		return cycleCount;
	}
}
