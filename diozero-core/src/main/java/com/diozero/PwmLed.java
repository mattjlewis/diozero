package com.diozero;

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

import com.diozero.api.PwmOutputDevice;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * PWM controlled LED. @see com.diozero.sampleapps.PwmLedTest PwmLedTest
 */
public class PwmLed extends PwmOutputDevice {
	/**
	 * @param gpio
	 *            The GPIO to which the LED is attached to.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public PwmLed(int gpio) throws RuntimeIOException {
		this(gpio, 0);
	}

	/**
	 * @param gpio
	 *            The GPIO to which the LED is attached to.
	 * @param initialValue
	 *            Initial PWM output value (range 0..1).
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public PwmLed(int gpio, float initialValue) throws RuntimeIOException {
		super(gpio, initialValue);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to provision this device.
	 * @param gpio
	 *            The GPIO to which the LED is attached to.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public PwmLed(PwmOutputDeviceFactoryInterface deviceFactory, int gpio) throws RuntimeIOException {
		this(deviceFactory, gpio, 0);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to provision this device.
	 * @param gpio
	 *            The GPIO to which the LED is attached to.
	 * @param initialValue
	 *            Initial PWM output value (range 0..1).
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public PwmLed(PwmOutputDeviceFactoryInterface deviceFactory, int gpio, float initialValue)
			throws RuntimeIOException {
		super(deviceFactory, gpio, initialValue);
	}

	/**
	 * Blink the LED on and off indefinitely.
	 * 
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void blink() throws RuntimeIOException {
		blink(1, 1, INFINITE_ITERATIONS, true);
	}

	/**
	 * Blink the LED on and off repeatedly.
	 * 
	 * @param onTime
	 *            On time in seconds.
	 * @param offTime
	 *            Off time in seconds.
	 * @param iterations
	 *            Number of iterations. Set to &lt;0 to blink indefinitely.
	 * @param background
	 *            If true start a background thread to control the blink and
	 *            return immediately. If false, only return once the blink
	 *            iterations have finished.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void blink(float onTime, float offTime, int iterations, boolean background) throws RuntimeIOException {
		onOffLoop(onTime, offTime, iterations, background);
	}

	/**
	 * Pulse the LED on and off indefinitely.
	 * 
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void pulse() throws RuntimeIOException {
		pulse(1, 50, INFINITE_ITERATIONS, true);
	}

	/**
	 * Pulse the LED on and off repeatedly.
	 * 
	 * @param fadeTime
	 *            Time from fully on to fully off.
	 * @param steps
	 *            Number of steps between fully on to fully off.
	 * @param iterations
	 *            Number of times to fade in and out.
	 * @param background
	 *            If true start a background thread to control the blink and
	 *            return immediately. If false, only return once the blink
	 *            iterations have finished.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void pulse(float fadeTime, int steps, int iterations, boolean background) throws RuntimeIOException {
		fadeInOutLoop(fadeTime, steps, iterations, background);
	}

	/**
	 * Return true if the PWM value is &gt;0.
	 * 
	 * @return True if &gt;0, false if 0.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public boolean isLit() throws RuntimeIOException {
		return isOn();
	}
}
