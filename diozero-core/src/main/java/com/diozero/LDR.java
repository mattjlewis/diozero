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


import com.diozero.api.AnalogInputDevice;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * <pre><code>
 * vRef     vLDR       GND
 *   |        |         |
 *   +---R1---+---LDR---+
 * </code></pre>
 * <p>When there is a lot of light the LDR has low resistance (vLdr ~ 0V).
 * When it is dark the resistance increases (vRef ~ vRef).</p>
 * <pre><code>
 * vLDR = vRef * (rLDR / (rLDR + R1))
 * IF R1 = 10,000ohm, vRef = 5v
 * When dark, if rLDR == 100,000ohm
 * vLDR = 5 * (100,000 / (100,000 + 10,000)) = 4.54V
 * When light, if rLDR == 100ohm
 * vLDR = 5 * (100 / (100 + 10,000)) = 0.0495V
 * 
 * rLDR = R1 / (vRef/vLDR - 1)
 * IF R1 = 10,000ohm, vRef = 5v
 * When dark, if vLDR=4V
 * rLDR = 10,000 / (5 / 4 - 1) = 40,000ohm
 * When light, if vLDR=1V
 * rLDR = 10,000 / (5 / 1 - 1) = 2,500ohm
 * </code></pre>
 */
public class LDR extends AnalogInputDevice {
	private float vRef;
	private float r1;
	
	public LDR(int pinNumber, float vRef, float r1) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, vRef, r1);
	}
	
	public LDR(AnalogInputDeviceFactoryInterface deviceFactory, int pinNumber,
			float vRef, float r1) throws RuntimeIOException {
		super(deviceFactory, pinNumber, vRef);
		
		this.vRef = vRef;
		this.r1 = r1;
	}
	
	public float getLdrResistance() throws RuntimeIOException {
		// Get the scaled value (voltage)
		float v_ldr = getScaledValue();
		
		return r1 / (vRef / v_ldr - 1);
	}
	
	public float getLuminosity() throws RuntimeIOException {
		// Get the scaled value (voltage)
		float v_ldr = getScaledValue();
		float r_ldr = getLdrResistance();
	
		// FIXME Can this be reliably calculated?
		
		// http://emant.com/316002.page
		// rLDR = 500 / Lux
		// Lux = (vRef*500/V(LDR) - 500) / R
		//double lux = (vRef * 500 / v_ldr - 500) / r;
		//Logger.debug("Lux={}", lux);
		// Or
		//double lux = 500 * (vRef - v_ldr) / (r + v_ldr);
		// Or...
		// I[lux] = 10000 / (R[kohms]*10)^(4/3)
		
		// https://learn.adafruit.com/photocells/measuring-light
		
		return r_ldr;
	}
}
