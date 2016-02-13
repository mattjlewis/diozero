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

import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogueInputDevice;
import com.diozero.api.DeviceFactoryHelper;
import com.diozero.api.LuminositySensorInterface;
import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * vRef      vLdr      GND
 *   |         |        |
 *   +---LDR---+---R1---+
 *
 * When there is a lot of light the LDR has low resistance (vLdr ~ vRef).
 * When it is dark the resistance increases (vRef ~ 0V).
 * vLDR = vRef * (rLDR / (rLDR + R1))
 * vLDR / vRef = rLDR / (rLDR + R1)
 * rLDR = R1 / (vRef/vLDR - 1)
 *
 * Given R1 = 1,000ohm, vRef = 5v
 * When dark, if rLDR == 100,000ohm
 * vLDR = 5 * (100,000 / (100,000 + 1,000)) = 4.95
 * When light, if rLDR == 100ohm
 * vLDR = 5 * (100 / (100 + 1,000)) = 0.45
 *
 * Given R1 = 10,000ohm, vRef = 5v
 * When dark, if R(LDR) == 100,000ohm
 * vLDR = 5 * (100,000 / (100,000 + 10,000)) = 4.54
 * When light, if R(LDR) == 100ohm
 * vLDR = 5 * (100 / (100 + 10,000)) = 0.049
 */
public class LDR extends AnalogueInputDevice implements LuminositySensorInterface {
	private float vRef;
	private float r1;
	
	public LDR(int pinNumber, float vRef, float r1) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, vRef, r1);
	}
	
	public LDR(AnalogueInputDeviceFactoryInterface deviceFactory, int pinNumber, float vRef, float r1) throws RuntimeIOException {
		super(deviceFactory, pinNumber);
		
		this.vRef = vRef;
		this.r1 = r1;
	}

	@Override
	public double getLuminosity() throws RuntimeIOException {
		double v_ldr = getValue();
		
		// http://emant.com/316002.page
		// rLDR = 500 / Lux
		// Voltage over the LDR vLDR = vRef * rLDR / (rLDR + R)
		// where R is the resister connected between the LDR and Ref voltage
		// Lux = (vRef*500/V(LDR) - 500) / R
		//double lux = (vRef * 500 / v_ldr - 500) / r;
		//Logger.debug("Lux={}", lux);
		// Or
		//double lux = 500 * (vRef - v_ldr) / (r + v_ldr);
		// Or...
		// I[lux] = 10000 / (R[kohms]*10)^(4/3)
		
		double r_ldr = r1 / (vRef / v_ldr - 1);
		// FIXME Check printf style formatting
		Logger.info("rLDR = {}", String.format("%.4f", Double.valueOf(r_ldr)));
		
		// https://learn.adafruit.com/photocells/measuring-light
		
		return v_ldr;
	}
}
