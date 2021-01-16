package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Button.java  
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

import com.diozero.api.*;
import com.diozero.api.function.Action;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;

/**
 * <p>Provides push button related utility methods.</p>
 * <p>From the <a href="https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/ButtonTest.java">ButtonTest</a> example:</p>
 * <img src="doc-files/Button.png" alt="Button">
 * <pre>
 * {@code
 * try (Button button = new Button(inputPin, GpioPullUpDown.PULL_UP)) {
 *   button.addListener(event -> Logger.debug("Event: {}", event));
 *   Logger.debug("Waiting for 10s - *** Press the button connected to input pin " + inputPin + " ***");
 *   SleepUtil.sleepSeconds(10);
 * }
 * }
 * </pre>
 * <p>Controlling an LED with a button <a href="https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/ButtonControlledLed.java">ButtonControlledLed</a>:</p>
 * <img src="doc-files/Button_LED.png" alt="Button controlled LED">
 * <pre>
 * {@code
 * try (Button button = new Button(buttonPin, GpioPullUpDown.PULL_UP); LED led = new LED(ledPin)) {
 *   button.whenPressed(led::on);
 *   button.whenReleased(led::off);
 *   Logger.info("Waiting for 10s - *** Press the button connected to pin {} ***", Integer.valueOf(buttonPin));
 *   SleepUtil.sleepSeconds(10);
 * }
 * }
 * </pre>
 */
public class Button extends DigitalInputDevice {
	/**
	 * Pull up / down configuration defaults to NONE.
	 * @param gpio GPIO to which the button is connected.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public Button(int gpio) throws RuntimeIOException {
		super(gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	/**
	 * @param gpio GPIO to which the button is connected.
	 * @param pud Pull up / down configuration (NONE, PULL_UP, PULL_DOWN).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public Button(int gpio, GpioPullUpDown pud) throws RuntimeIOException {
		super(gpio, pud, GpioEventTrigger.BOTH);
	}

	/**
	 * @param deviceFactory Device factory to use to contruct the device.
	 * @param gpio GPIO for the button.
	 * @param pud Pull up / down configuration (NONE, PULL_UP, PULL_DOWN).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public Button(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud) throws RuntimeIOException {
		super(deviceFactory, gpio, pud, GpioEventTrigger.BOTH);
	}
	
	/**
	 * Get the current state.
	 * @return Return true if the button is currently pressed.
	 */
	public boolean isPressed() {
		return isActive();
	}
	
	/**
	 * Get the current state.
	 * @return Return true if the button is currently released.
	 */
	public boolean isReleased() {
		return !isActive();
	}
	
	/**
	 * Action to perform when the button is pressed.
	 * @param action Action function to invoke.
	 */
	public void whenPressed(Action action) {
		whenActivated(action);
	}
	
	/**
	 * Action to perform when the button is released.
	 * @param action Action function to invoke.
	 */
	public void whenReleased(Action action) {
		whenDeactivated(action);
	}
}
