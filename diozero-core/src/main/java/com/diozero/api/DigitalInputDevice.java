package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DigitalInputDevice.java
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

import org.tinylog.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * Represents a generic digital input device.
 */
public class DigitalInputDevice extends AbstractDigitalInputDevice {
	/**
	 * Digital input device builder. Default values:
	 * <ul>
	 * <li>pud: {@link GpioPullUpDown#NONE}</li>
	 * <li>trigger: {@link GpioEventTrigger#BOTH}</li>
	 * <li>activeHigh: set to false if pud == {@link GpioPullUpDown#PULL_UP},
	 * otherwise true (assumes normally open wiring configuration)</li>
	 * <li>deviceFactory: {@link DeviceFactoryHelper#getNativeDeviceFactory}</li>
	 * </ul>
	 *
	 * Either a GPIO number or a {@link PinInfo} instance must be specified. Using a
	 * PinInfo instance allows input devices to be identified by either physical pin
	 * number or GPIO chip and line offset.
	 *
	 * The optional activeHigh parameter default value assumes a normally open
	 * wiring configuration, however, this can be overridden for normally closed
	 * configurations as well as scenarios where pud is {@link GpioPullUpDown#NONE}
	 * and an external pull up/down resistor is used.
	 */
	public static class Builder {
		/**
		 * Create a new DigitalInputDevice builder instance
		 *
		 * @param gpio The GPIO to be used for the new DigitalInputDevice
		 * @return A new DigitalInputDevice builder instance
		 */
		public static Builder builder(int gpio) {
			return new Builder(gpio);
		}

		/**
		 * Create a new DigitalInputDevice builder instance
		 *
		 * @param pinInfo The pin to be used for the new DigitalInputDevice
		 * @return A new DigitalInputDevice builder instance
		 */
		public static Builder builder(PinInfo pinInfo) {
			return new Builder(pinInfo);
		}

		private Integer gpio;
		private PinInfo pinInfo;
		private GpioPullUpDown pud = GpioPullUpDown.NONE;
		private GpioEventTrigger trigger = GpioEventTrigger.BOTH;
		private Boolean activeHigh;
		private GpioDeviceFactoryInterface deviceFactory;

		public Builder(int gpio) {
			this.gpio = Integer.valueOf(gpio);
		}

		public Builder(PinInfo pinInfo) {
			this.pinInfo = pinInfo;
		}

		public Builder setPullUpDown(GpioPullUpDown pud) {
			this.pud = pud;
			return this;
		}

		public Builder setTrigger(GpioEventTrigger trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder setActiveHigh(boolean activeHigh) {
			this.activeHigh = Boolean.valueOf(activeHigh);
			return this;
		}

		public Builder setDeviceFactory(GpioDeviceFactoryInterface deviceFactory) {
			this.deviceFactory = deviceFactory;
			return this;
		}

		public DigitalInputDevice build() throws RuntimeIOException, NoSuchDeviceException {
			// Determine activeHigh from pud if not explicitly set
			if (activeHigh == null) {
				activeHigh = Boolean.valueOf(pud != GpioPullUpDown.PULL_UP);
			}

			// Default to the native device factory if not set
			if (deviceFactory == null) {
				deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
			}

			if (pinInfo == null) {
				pinInfo = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio.intValue());
			}

			return new DigitalInputDevice(deviceFactory, pinInfo, pud, trigger, activeHigh.booleanValue());
		}
	}

	private GpioDigitalInputDeviceInterface delegate;
	private GpioPullUpDown pud;
	private GpioEventTrigger trigger;

	/**
	 * @param gpio GPIO to which the device is connected.
	 * @throws RuntimeIOException If an I/O error occurs.
	 */
	public DigitalInputDevice(int gpio) throws RuntimeIOException, NoSuchDeviceException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	/**
	 * @param gpio    GPIO to which the device is connected
	 * @param pud     Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN
	 * @param trigger Event trigger configuration, values: NONE, RISING, FALLING,
	 *                BOTH
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public DigitalInputDevice(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger)
			throws RuntimeIOException, NoSuchDeviceException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pud, trigger);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this digital input
	 *                      device
	 * @param gpio          GPIO to which the device is connected
	 * @param pud           Pull up/down configuration, values: NONE, PULL_UP,
	 *                      PULL_DOWN
	 * @param trigger       Event trigger configuration, values: NONE, RISING,
	 *                      FALLING, BOTH
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public DigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException, NoSuchDeviceException {
		this(deviceFactory, deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio), pud, trigger,
				pud != GpioPullUpDown.PULL_UP);
	}

	/**
	 * @param deviceFactory Device factory to use to provision this digital input
	 *                      device
	 * @param pinInfo       Information about the GPIO pin to which the device is
	 *                      connected
	 * @param pud           Pull up/down configuration, values: NONE, PULL_UP,
	 *                      PULL_DOWN
	 * @param trigger       Event trigger configuration, values: NONE, RISING,
	 *                      FALLING, BOTH
	 * @param activeHigh    Set to true if digital 1 is to be treated as active
	 * @throws RuntimeIOException If an I/O error occurs.
	 */
	public DigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger, boolean activeHigh) throws RuntimeIOException, NoSuchDeviceException {
		super(pinInfo, activeHigh);

		this.delegate = deviceFactory.provisionDigitalInputDevice(pinInfo, pud, trigger);
		this.pud = pud;
		this.trigger = trigger;
	}

	@Override
	public void close() {
		Logger.trace("close()");
		if (delegate.isOpen()) {
			removeAllListeners();
			delegate.close();
		}
	}

	public String getName() {
		return delegate.getKey();
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
	 * Read the current on/off state for this device taking into account the pull up
	 * / down configuration. If the input is pulled up {@code isActive()} will
	 * return {@code true} when when the value is {@code false}.
	 *
	 * @return Device active state.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public boolean isActive() throws RuntimeIOException {
		return delegate.getValue() == activeHigh;
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
