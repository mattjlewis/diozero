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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * <p>
 * Provides utility methods for controlling a Light Emitting Diode (LED).
 * Connect the cathode (short leg, flat side) of the LED to a ground pin;
 * connect the anode (longer leg) to a limiting resistor; connect the other side
 * of the limiting resistor to a GPIO pin (the limiting resistor can be placed
 * either side of the LED). Example LED control, taken from
 * {@link com.diozero.sampleapps.LEDTest LEDTest}:
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
	 * @param pinNumber
	 *            GPIO pin to which the LED is connected.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(int pinNumber) throws RuntimeIOException {
		super(pinNumber);
	}

	/**
	 * @param pinNumber
	 *            GPIO pin to which the LED is connected.
	 * @param activeHigh
	 *            Set to true if a high output value represents on.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(int pinNumber, boolean activeHigh) throws RuntimeIOException {
		super(pinNumber, activeHigh, !activeHigh);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to construct the device.
	 * @param pinNumber
	 *            GPIO pin to which the LED is connected.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(GpioDeviceFactoryInterface deviceFactory, int pinNumber) throws RuntimeIOException {
		super(deviceFactory, pinNumber, true, false);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to construct the device.
	 * @param pinNumber
	 *            GPIO pin to which the LED is connected.
	 * @param activeHigh
	 *            Set to true if a high output value represents on.
	 * @param initialValue
	 *            Initial value.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LED(GpioDeviceFactoryInterface deviceFactory, int pinNumber, boolean activeHigh, boolean initialValue) {
		super(deviceFactory, pinNumber, activeHigh, initialValue);
	}

	/**
	 * Blink indefinitely with 1 second on and 1 second off.
	 * 
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public void blink() throws RuntimeIOException {
		blink(1, 1, INFINITE_ITERATIONS, true);
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
		onOffLoop(onTime, offTime, n, background);
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
