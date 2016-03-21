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


import org.junit.Assert;
import org.junit.Test;

import com.diozero.util.ServoUtil;

@SuppressWarnings("static-method")
public class ServoTest {
	private static final double TOWERPRO_SG5010_MIN_MS = 1;
	private static final double TOWERPRO_SG5010_MAX_MS = 2;
	private static final double TOWERPRO_SG90_MIN_MS = 0.6;
	private static final double TOWERPRO_SG90_MAX_MS = 2.5;
	
	@Test
	public void test() {
		int pwm_freq = 60;
		int bits = 12;
		int range = (int)Math.pow(2, bits);
		double ms_per_bit = ServoUtil.calcPulseMsPerBit(pwm_freq, range);
		
		double pulse_width_ms = TOWERPRO_SG5010_MIN_MS;
		int servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG5010 Min: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(245, servo_pulse);
		
		pulse_width_ms = TOWERPRO_SG5010_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG5010 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(491, servo_pulse);
		
		pulse_width_ms = TOWERPRO_SG90_MIN_MS;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Min: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(147, servo_pulse);
		
		pulse_width_ms = TOWERPRO_SG90_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(614, servo_pulse);
		
		// Error - value greater than range...
		pulse_width_ms = 20;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		
		double max_pulse_width = 16.666666667;
		servo_pulse = ServoUtil.calcServoPulseBits(max_pulse_width, pwm_freq, range);
		Assert.assertEquals(range, servo_pulse);
		System.out.println(servo_pulse);
		
		System.out.println("Max pulse width=" + (1000.0 / pwm_freq) + "ms");
		
		double val = TOWERPRO_SG90_MIN_MS * pwm_freq / 1000f;
		System.out.println("Pulse width of " + TOWERPRO_SG90_MIN_MS + "ms is " + (val * 100f) + "%");
		Assert.assertEquals(0.036, val, 0.001);
		
		pulse_width_ms = val * 1000f / pwm_freq;
		System.out.println("Val of " + (val * 100f) + "% is " + TOWERPRO_SG90_MIN_MS + "ms");
		Assert.assertEquals(TOWERPRO_SG90_MIN_MS, pulse_width_ms, 0.01);
	}
}
