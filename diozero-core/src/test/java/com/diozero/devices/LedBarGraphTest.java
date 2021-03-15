package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     LedBarGraphTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diozero.util.RangeUtil;

@SuppressWarnings("static-method")
public class LedBarGraphTest {
	private static final float DELTA_F = 0.0001f;
	
	@Test
	public void setValueTest() {
		float[] leds = new float[10];
		
		float value = 0.8f;
		setValues(leds, value);
		int i;
		for (i=0; i<leds.length; i++) {
			Assertions.assertEquals(i<8 ? 1 : 0, leds[i], DELTA_F);
		}
		
		value = 0.5f;
		setValues(leds, value);
		for (i=0; i<leds.length; i++) {
			Assertions.assertEquals(i<5 ? 1 : 0, leds[i], DELTA_F);
		}
		
		value = 0.55f;
		setValues(leds, value);
		for (i=0; i<5; i++) {
			Assertions.assertEquals(1, leds[i], DELTA_F);
		}
		Assertions.assertEquals(0.5, leds[i++], DELTA_F);
		for (; i<leds.length; i++) {
			Assertions.assertEquals(0, leds[i], DELTA_F);
		}
		
		value = 0.14f;
		setValues(leds, value);
		Assertions.assertEquals(1, leds[0], DELTA_F);
		Assertions.assertEquals(0.4, leds[1], DELTA_F);
		for (i=2; i<leds.length; i++) {
			Assertions.assertEquals(0, leds[i], DELTA_F);
		}
		
		value = 2f;
		setValues(leds, value);
		for (i=0; i<leds.length; i++) {
			Assertions.assertEquals(1, leds[i], DELTA_F);
		}
		
		value = -0.8f;
		setValues(leds, value);
		for (i=0; i<leds.length; i++) {
			Assertions.assertEquals(i<2 ? 0 : 1, leds[i], DELTA_F);
		}
		
		value = -0.5f;
		setValues(leds, value);
		for (i=0; i<leds.length; i++) {
			Assertions.assertEquals(i<5 ? 0 : 1, leds[i], DELTA_F);
		}
		
		value = -0.55f;
		setValues(leds, value);
		for (i=0; i<4; i++) {
			Assertions.assertEquals(0, leds[i], DELTA_F);
		}
		Assertions.assertEquals(0.5, leds[i++], DELTA_F);
		for (; i<leds.length; i++) {
			Assertions.assertEquals(1, leds[i], DELTA_F);
		}
		
		value = -0.14f;
		setValues(leds, value);
		for (i=0; i<8; i++) {
			Assertions.assertEquals(0, leds[i], DELTA_F);
		}
		Assertions.assertEquals(0.4, leds[i++], DELTA_F);
		Assertions.assertEquals(1, leds[i++], DELTA_F);
		
		value = -2f;
		setValues(leds, value);
		for (i=0; i<leds.length; i++) {
			Assertions.assertEquals(1, leds[i], DELTA_F);
		}
	}
	
	private static void setValues(float[] leds, float value) {
		for (int i=0; i<leds.length; i++) {
			leds[value < 0 ? leds.length-i-1 : i] = RangeUtil.constrain(leds.length * Math.abs(value) - i, 0, 1);
		}
		System.out.println(value + ": " + Arrays.toString(leds));
	}
}
