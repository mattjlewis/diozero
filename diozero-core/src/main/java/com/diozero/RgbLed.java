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


import java.io.Closeable;

import org.pmw.tinylog.Logger;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Three pin controlled RGB LED.
 */
public class RgbLed implements Closeable {
	private LED redLED;
	private LED greenLED;
	private LED blueLED;
	
	/**
	 * @param redPin GPIO pin number for the red LED.
	 * @param greenPin GPIO pin number for the green LED.
	 * @param bluePin GPIO pin number for the blue LED.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public RgbLed(int redPin, int greenPin, int bluePin) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), redPin, greenPin, bluePin);
	}
	
	/**
	 * @param deviceFactory Device factory to use to provision this device.
	 * @param redPin GPIO pin number for the red LED.
	 * @param greenPin GPIO pin number for the green LED.
	 * @param bluePin GPIO pin number for the blue LED.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public RgbLed(GpioDeviceFactoryInterface deviceFactory, int redPin, int greenPin, int bluePin) throws RuntimeIOException {
		redLED = new LED(deviceFactory, redPin);
		greenLED = new LED(deviceFactory, greenPin);
		blueLED = new LED(deviceFactory, bluePin);
	}
	
	@Override
	public void close() {
		Logger.debug("close()");
		redLED.close();
		greenLED.close();
		blueLED.close();
	}
	
	/**
	 * Get the state of all LEDs.
	 * @return Boolean array (red, green, blue).
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public boolean[] getState() throws RuntimeIOException {
		return new boolean[] { redLED.isOn(), greenLED.isOn(), blueLED.isOn() };
	}
	
	/**
	 * Set the state of all LEDs.
	 * @param red Red LED state.
	 * @param green Green LED state.
	 * @param blue Blue LED state.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void setState(boolean red, boolean green, boolean blue) throws RuntimeIOException {
		redLED.setOn(red);
		greenLED.setOn(green);
		blueLED.setOn(blue);
	}
	
	/**
	 * Turn all LEDs on.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void on() throws RuntimeIOException {
		redLED.on();
		greenLED.on();
		blueLED.on();
	}
	
	/**
	 * Turn all LEDs off.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void off() throws RuntimeIOException {
		redLED.off();
		greenLED.off();
		blueLED.off();
	}
	
	/**
	 * Toggle the state of all LEDs.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void toggle() throws RuntimeIOException {
		redLED.toggle();
		greenLED.toggle();
		blueLED.toggle();
	}
}
