package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SevenSegmentDisplay.java
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

import java.util.Arrays;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * <p>
 * Multi-digit 7-segment display. Tested with this <a href=
 * "https://cdn-shop.adafruit.com/datasheets/865datasheet.pdf">Luckylight
 * model</a> from <a href=
 * "https://shop.pimoroni.com/products/7-segment-clock-display-0-56-digit-height?variant=31551016894547">Pimoroni</a>.
 * </p>
 *
 * <p>
 * Segments are assumed to be connected in the following order. Note decimal
 * point (DP) and colon (Col) not yet implemented. Connect each segment via a
 * current limiting resistor, e.g. 220 or 330 Ohms.
 * </p>
 *
 * Link:
 * https://www.electronics-tutorials.ws/blog/7-segment-display-tutorial.html
 *
 * <pre>
 *     A
 *   F   B    Col
 *     G
 *   E   C    Col
 *     D   DP
 * </pre>
 */
public class SevenSegmentDisplay implements DeviceInterface {
	private static final boolean[][] NUMBERS = { //
			{ true, true, true, true, true, true, false }, // 0
			{ false, true, true, false, false, false, false }, // 1
			{ true, true, false, true, true, false, true }, // 2
			{ true, true, true, true, false, false, true }, // 3
			{ false, true, true, false, false, true, true }, // 4
			{ true, false, true, true, false, true, true }, // 5
			{ true, false, true, true, true, true, true }, // 6
			{ true, true, true, false, false, false, false }, // 7
			{ true, true, true, true, true, true, true }, // 8
			{ true, true, true, true, false, true, true }, // 9
	};

	// Length 7 - one for each segment
	private DigitalOutputDevice[] segments;
	private DigitalOutputDevice decimalPoint;
	private DigitalOutputDevice semicolon;
	// Control which digit is displayed, arbitrary length
	private DigitalOutputDevice[] digitControl;

	public SevenSegmentDisplay(int aGpio, int bGpio, int cGpio, int dGpio, int eGpio, int fGpio, int gGpio,
			int[] digitControlGpios) {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), aGpio, bGpio, cGpio, dGpio, eGpio, fGpio, gGpio,
				digitControlGpios);
	}

	public SevenSegmentDisplay(GpioDeviceFactoryInterface deviceFactory, int aGpio, int bGpio, int cGpio, int dGpio,
			int eGpio, int fGpio, int gGpio, int[] digitControlGpios) {
		// TODO Include DP and Colon
		segments = new DigitalOutputDevice[7];
		int i = 0;
		segments[i++] = new DigitalOutputDevice(deviceFactory, aGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, bGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, cGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, dGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, eGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, fGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, gGpio, true, false);

		digitControl = new DigitalOutputDevice[digitControlGpios.length];
		for (i = 0; i < digitControlGpios.length; i++) {
			digitControl[i] = new DigitalOutputDevice(deviceFactory, digitControlGpios[i], false, false);
		}
	}

	@Override
	public void close() {
		Arrays.asList(digitControl).forEach(DeviceInterface::close);
		Arrays.asList(segments).forEach(DeviceInterface::close);
	}

	public void displayNumbers(int value, boolean[] onDigits) {
		if (onDigits.length > digitControl.length) {
			throw new IllegalArgumentException("Too many digits specified (" + onDigits.length
					+ "), array length must be 1.." + digitControl.length);
		}
		if (value > NUMBERS.length - 1) {
			throw new IllegalArgumentException("Invalid value " + value + " - only numbers 0..9 are supported");
		}

		for (int i = 0; i < digitControl.length; i++) {
			digitControl[i].setOn(onDigits[i]);
		}

		boolean[] values = NUMBERS[value];
		for (int i = 0; i < segments.length; i++) {
			segments[i].setOn(values[i]);
		}
	}

	public void enableDigit(int digit) {
		for (int i = 0; i < digitControl.length; i++) {
			digitControl[i].setOn(i == digit);
		}
	}

	public void displayNumber(int value) {
		if (value > NUMBERS.length - 1) {
			throw new IllegalArgumentException("Invalid value " + value + " - only numbers 0..9 are supported");
		}

		boolean[] values = NUMBERS[value];
		for (int i = 0; i < segments.length; i++) {
			segments[i].setOn(values[i]);
		}
	}

	public void display(boolean[] values) {
		if (values.length != segments.length) {
			throw new IllegalArgumentException(
					"Invalid values array length (" + values.length + ") - must be " + segments.length);
		}

		for (int i = 0; i < segments.length; i++) {
			segments[i].setOn(values[i]);
		}
	}
}
