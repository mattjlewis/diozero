package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     Button.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.NoSuchDeviceException;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * <p>
 * Provides push button related utility methods.
 * </p>
 * <p>
 * From the <a href=
 * "https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/ButtonTest.java">ButtonTest</a>
 * example:
 * </p>
 * <img src="doc-files/Button.png" alt="Button">
 *
 * <pre>
 * {@code
 * try (Button button = new Button(inputPin, GpioPullUpDown.PULL_UP)) {
 * 	button.addListener(event -> Logger.debug("Event: {}", event));
 * 	Logger.debug("Waiting for 10s - *** Press the button connected to input pin " + inputPin + " ***");
 * 	SleepUtil.sleepSeconds(10);
 * }
 * }
 * </pre>
 * <p>
 * Controlling an LED with a button <a href=
 * "https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/ButtonControlledLed.java">ButtonControlledLed</a>:
 * </p>
 * <img src="doc-files/Button_LED.png" alt="Button controlled LED">
 *
 * <pre>
 * {@code
 * try (Button button = new Button(buttonPin, GpioPullUpDown.PULL_UP); LED led = new LED(ledPin)) {
 * 	button.whenPressed(nanoTime -> led::on);
 * 	button.whenReleased(nanoTime -> led::off);
 * 	Logger.info("Waiting for 10s - *** Press the button connected to pin {} ***", Integer.valueOf(buttonPin));
 * 	SleepUtil.sleepSeconds(10);
 * }
 * }
 * </pre>
 */
// FIXME Use composition instead of inheritance...
public class Button extends DigitalInputDevice {
	/**
	 * Button builder. Default values:
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
		 * Create a new Button builder instance
		 *
		 * @param gpio The GPIO to be used for the new DigitalInputDevice
		 * @return A new Button builder instance
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

		private Builder(int gpio) {
			this.gpio = Integer.valueOf(gpio);
		}

		private Builder(PinInfo pinInfo) {
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

		public Builder setGpioDeviceFactoryInterface(GpioDeviceFactoryInterface deviceFactory) {
			this.deviceFactory = deviceFactory;
			return this;
		}

		public Button build() throws RuntimeIOException, NoSuchDeviceException {
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

			return new Button(deviceFactory, pinInfo, pud, trigger, activeHigh.booleanValue());
		}
	}

	/**
	 * @param gpio GPIO to which the device is connected.
	 * @throws RuntimeIOException If an I/O error occurs.
	 */
	public Button(int gpio) throws RuntimeIOException, NoSuchDeviceException {
		super(gpio);
	}

	/**
	 * @param gpio GPIO to which the button is connected.
	 * @param pud  Pull up / down configuration (NONE, PULL_UP, PULL_DOWN).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public Button(int gpio, GpioPullUpDown pud) throws RuntimeIOException {
		super(gpio, pud, GpioEventTrigger.BOTH);
	}

	/**
	 * @param gpio    GPIO to which the device is connected
	 * @param pud     Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN
	 * @param trigger Event trigger configuration, values: NONE, RISING, FALLING,
	 *                BOTH
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public Button(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger)
			throws RuntimeIOException, NoSuchDeviceException {
		super(gpio, pud, trigger);
	}

	public Button(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud) {
		super(deviceFactory, gpio, pud, GpioEventTrigger.BOTH);
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
	public Button(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud, GpioEventTrigger trigger)
			throws RuntimeIOException, NoSuchDeviceException {
		super(deviceFactory, gpio, pud, trigger);
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
	public Button(GpioDeviceFactoryInterface deviceFactory, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger, boolean activeHigh) throws RuntimeIOException, NoSuchDeviceException {
		super(deviceFactory, pinInfo, pud, trigger, activeHigh);
	}

	/**
	 * Get the current state.
	 *
	 * @return Return true if the button is currently pressed.
	 */
	public boolean isPressed() {
		return isActive();
	}

	/**
	 * Get the current state.
	 *
	 * @return Return true if the button is currently released.
	 */
	public boolean isReleased() {
		return !isActive();
	}

	/**
	 * Action to perform when the button is pressed.
	 *
	 * @param consumer Callback function to invoke when pressed (long parameter is
	 *                 nanoseconds time).
	 */
	public void whenPressed(LongConsumer consumer) {
		whenActivated(consumer);
	}

	/**
	 * Action to perform when the button is released.
	 *
	 * @param consumer Callback function to invoke when pressed (long parameter is
	 *                 nanoseconds time).
	 */
	public void whenReleased(LongConsumer consumer) {
		whenDeactivated(consumer);
	}
}
