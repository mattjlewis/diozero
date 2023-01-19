package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     LDR.java
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


import com.diozero.api.AnalogInputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * <p>
 * Generic <a href="https://en.wikipedia.org/wiki/Photoresistor">Photoresistor /
 * Light-Dependent-Resistor (LDR)</a>.<br>
 * Wiring:
 * </p>
 * 
 * <pre>
 * vRef     vLDR       GND
 *   |        |         |
 *   +---R1---+---LDR---+
 * </pre>
 * <p>
 * When there is a lot of light the LDR has low resistance (vLdr ~ 0V). When it
 * is dark the resistance increases (vLdr ~ vRef).
 * </p>
 * <p>
 * {@code vLDR = vRef * (rLDR / (rLDR + R1))}
 * </p>
 * <p>
 * Given R1 = 10,000ohm and vRef = 5v:<br>
 * When dark, if rLDR == 100,000 Ohm:
 * {@code vLDR = 5 * (100,000 / (100,000 + 10,000)) = 4.54V}<br>
 * When light, if rLDR == 100 Ohm:
 * {@code vLDR = 5 * (100 / (100 + 10,000)) = 0.0495V}
 * </p>
 * 
 * <p>
 * {@code rLDR = R1 / (vRef / vLDR - 1)}
 * </p>
 * <p>
 * Given R1 = 10,000ohm and vRef = 5v:<br>
 * When dark, if vLDR=4V: {@code rLDR = 10,000 / (5 / 4 - 1) = 40,000 Ohm}<br>
 * When light, if vLDR=1V: {@code rLDR = 10,000 / (5 / 1 - 1) = 2,500 Ohm}<br>
 * </p>
 */
public class LDR extends AnalogInputDevice {
	private float r1;

	/**
	 * @param gpio
	 *            GPIO to which the LDR is connected.
	 * @param r1
	 *            Resistor between the LDR and ground.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LDR(int gpio, float r1) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, r1);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to construct the device.
	 * @param gpio
	 *            GPIO to which the LDR is connected.
	 * @param r1
	 *            Resistor between the LDR and ground.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public LDR(AnalogInputDeviceFactoryInterface deviceFactory, int gpio, float r1) throws RuntimeIOException {
		super(deviceFactory, gpio);

		this.r1 = r1;
	}

	/**
	 * Read the resistance across the LDR.
	 * 
	 * @return Resistance (Ohms)
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	public float getLdrResistance() throws RuntimeIOException {
		// Get the scaled value (voltage)
		float v_ldr = getScaledValue();

		return r1 / (getRange() / v_ldr - 1);
	}

	/**
	 * Read the LDR luminosity. <strong>Not yet implemented</strong> (not sure
	 * if this can be reliably implemented).
	 * 
	 * @return Luminosity (lux).
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	private float getLuminosity() throws RuntimeIOException {
		// Get the scaled value (voltage)
		float v_ldr = getScaledValue();
		float r_ldr = getLdrResistance();

		// FIXME Can this be reliably calculated?

		// http://emant.com/316002.page
		// rLDR = 500 / Lux
		// Lux = (vRef*500/V(LDR) - 500) / R
		// double lux = (vRef * 500 / v_ldr - 500) / r;
		// Logger.debug("Lux={}", lux);
		// Or
		// double lux = 500 * (vRef - v_ldr) / (r + v_ldr);
		// Or...
		// I[lux] = 10000 / (R[kohms]*10)^(4/3)

		// https://learn.adafruit.com/photocells/measuring-light

		return r_ldr;
	}
}
