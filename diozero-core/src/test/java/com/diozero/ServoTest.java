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

public class ServoTest {
	private static final double TOWERPRO_SG5010_MIN_MS = 1;
	private static final double TOWERPRO_SG5010_MAX_MS = 2;
	private static final double TOWERPRO_SG90_MIN_MS = 0.5;
	private static final double TOWERPRO_SG90_MAX_MS = 2.4;
	
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		int bits = 12;
		int range = (int)Math.pow(2, bits);
		double ms_per_bit = ServoUtil.calcPulseMsPerBit(60, range);
		
		double on = TOWERPRO_SG5010_MIN_MS;
		int servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG5010 Min: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 246);
		
		on = TOWERPRO_SG5010_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG5010 Max: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 492);
		
		on = TOWERPRO_SG90_MIN_MS;
		servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG90 Min: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 123);
		
		on = TOWERPRO_SG90_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulse(on, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + on + ", servo_pulse=" + servo_pulse);
		Assert.assertEquals(servo_pulse, 590);
	}
}
