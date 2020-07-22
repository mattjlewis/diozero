package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     LED.java  
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


import com.diozero.api.Action;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.provider.GpioDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * <p>
 * Provides utility methods for controlling a Light Emitting Diode (LED).
 * Connect the cathode (short leg, flat side) of the LED to a ground pin;
 * connect the anode (longer leg) to a limiting resistor; connect the other side
 * of the limiting resistor to a GPIO pin (the limiting resistor can be placed
 * either side of the LED). Example LED control, taken from
 * <a href="https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/LEDTest.java">LEDTest</a>:
 * </p>
 * 
 * <pre>
 * {@code
 *try (LED led = new LED(pin)) {
 *	led.on();
 *	SleepUtil.sleepSeconds(.5);
 *	led.off();
 *	SleepUtil.sleepSeconds(.5);
 *	led.toggle();
 *	SleepUtil.sleepSeconds(.5);
 *	led.toggle();
 *	SleepUtil.sleepSeconds(.5);
 *	led.blink(0.5f, 0.5f, 10, false);
 *}}
 * </pre>
 */
public class LED extends DigitalOutputDevice {

	/**
	 * @param gpio
	 *            GPIO to which the LED is connected.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(int gpio) throws RuntimeIOException {
		super(gpio);
	}

	/**
	 * @param gpio
	 *            GPIO to which the LED is connected.
	 * @param activeHigh
	 *            Set to true if a high output value represents on.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(int gpio, boolean activeHigh) throws RuntimeIOException {
		super(gpio, activeHigh, !activeHigh);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to construct the device.
	 * @param gpio
	 *            GPIO to which the LED is connected.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(GpioDeviceFactoryInterface deviceFactory, int gpio) throws RuntimeIOException {
		super(deviceFactory, gpio, true, false);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to construct the device.
	 * @param gpio
	 *            GPIO to which the LED is connected.
	 * @param activeHigh
	 *            Set to true if a high output value represents on.
	 * @param initialValue
	 *            Initial value.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(GpioDeviceFactoryInterface deviceFactory, int gpio, boolean activeHigh, boolean initialValue) {
		super(deviceFactory, gpio, activeHigh, initialValue);
	}

	/**
	 * Blink indefinitely with 1 second on and 1 second off.
	 * 
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void blink() throws RuntimeIOException {
		blink(1, 1, INFINITE_ITERATIONS, true, null);
	}

	/**
	 * Blink indefinitely with 1 second on and 1 second off.
	 * @param stopAction
	 *             Action to invoke when the animation stops.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void blink(Action stopAction) throws RuntimeIOException {
		blink(1, 1, INFINITE_ITERATIONS, true, stopAction);
	}

	/**
	 * Blink.
	 * 
	 * @param onTime
	 *            On time in seconds.
	 * @param offTime
	 *            Off time in seconds.
	 * @param n
	 *            Number of iterations. Set to &lt;0 to blink indefinitely.
	 * @param background
	 *            If true start a background thread to control the blink and
	 *            return immediately. If false, only return once the blink
	 *            iterations have finished.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void blink(float onTime, float offTime, int n, boolean background) throws RuntimeIOException {
		blink(onTime, offTime, n, background, null);
	}

	/**
	 * Blink.
	 * 
	 * @param onTime
	 *            On time in seconds.
	 * @param offTime
	 *            Off time in seconds.
	 * @param n
	 *            Number of iterations. Set to &lt;0 to blink indefinitely.
	 * @param background
	 *            If true start a background thread to control the blink and
	 *            return immediately. If false, only return once the blink
	 *            iterations have finished.
	 * @param stopAction
	 *            Action to invoke when the animation stops.
	 * @throws RuntimeIOException
	 *            If an I/O error occurred.
	 */
	public void blink(float onTime, float offTime, int n, boolean background, Action stopAction) throws RuntimeIOException {
		onOffLoop(onTime, offTime, n, background, stopAction);
	}

	/**
	 * Return true if the LED is currently on.
	 * 
	 * @return True if the LED is on, false if off.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public boolean isLit() throws RuntimeIOException {
		return isOn();
	}
}
