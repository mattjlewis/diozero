package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     RangeUtilTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class RangeUtilTest {
	private static final float DELTA_F = 0.001f;

	@Test
	public void testMap() {
		float from_low = 0f;
		float from_high = 1f;
		int to_low_int = 0;
		int to_high_int = 180;

		Assertions.assertEquals(to_low_int, RangeUtil.map(from_low, from_low, from_high, to_low_int, to_high_int));
		Assertions.assertEquals((to_low_int + to_high_int) / 2,
				RangeUtil.map((from_low + from_high) / 2, from_low, from_high, to_low_int, to_high_int));
		Assertions.assertEquals(to_high_int, RangeUtil.map(from_high, from_low, from_high, to_low_int, to_high_int));
		Assertions.assertEquals(to_low_int, RangeUtil.map(from_low - 10, from_low, from_high, to_low_int, to_high_int),
				DELTA_F);
		Assertions.assertEquals(to_high_int, RangeUtil.map(from_high + 10, from_low, from_high, to_low_int, to_high_int),
				DELTA_F);
		Assertions.assertEquals(-to_high_int,
				RangeUtil.map(from_low - from_high, from_low, from_high, to_low_int, to_high_int, false), DELTA_F);
		Assertions.assertEquals(2 * to_high_int,
				RangeUtil.map(2 * from_high, from_low, from_high, to_low_int, to_high_int, false), DELTA_F);

		from_low = -90;
		from_high = 90;
		float to_low_float = 1f;
		float to_high_float = 2f;
		Assertions.assertEquals(to_low_float, RangeUtil.map(from_low, from_low, from_high, to_low_float, to_high_float),
				DELTA_F);
		Assertions.assertEquals((to_low_float + to_high_float) / 2,
				RangeUtil.map((from_low + from_high) / 2, from_low, from_high, to_low_float, to_high_float), DELTA_F);
		Assertions.assertEquals(to_high_float, RangeUtil.map(from_high, from_low, from_high, to_low_float, to_high_float),
				DELTA_F);
		Assertions.assertEquals(to_low_float,
				RangeUtil.map(from_low - 10, from_low, from_high, to_low_float, to_high_float), DELTA_F);
		Assertions.assertEquals(to_high_float,
				RangeUtil.map(from_high + 10, from_low, from_high, to_low_float, to_high_float), DELTA_F);
		Assertions.assertEquals(0.5,
				RangeUtil.map(from_low - from_high, from_low, from_high, to_low_float, to_high_float, false), DELTA_F);
		Assertions.assertEquals(2.5, RangeUtil.map(2 * from_high, from_low, from_high, to_low_float, to_high_float, false),
				DELTA_F);
	}

	@Test
	public void testServoRange() {
		float min_pulse_width_ms = 0.75f;
		float max_pulse_width_ms = 2.25f;
		float minus90_pulse_width_ms = 1f;
		float plus90_pulse_width_ms = 2f;

		Assertions.assertEquals(
				RangeUtil.map(min_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false), -45);
		Assertions.assertEquals(
				RangeUtil.map(max_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false), 225);
		Assertions.assertEquals(
				RangeUtil.map(minus90_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false), 0);
		Assertions.assertEquals(
				RangeUtil.map(plus90_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false),
				180);
		Assertions.assertEquals(RangeUtil.map((minus90_pulse_width_ms + plus90_pulse_width_ms) / 2, minus90_pulse_width_ms,
				plus90_pulse_width_ms, 0, 180, false), 90);
		
		Assertions.assertEquals(0, RangeUtil.map(-1, -1, 1, 0, 180));
		Assertions.assertEquals(90, RangeUtil.map(0, -1, 1, 0, 180));
		Assertions.assertEquals(180, RangeUtil.map(1, -1, 1, 0, 180));
		
		int servo_centre = 90;
		float value = -1;
		float pz_value = Math.round(servo_centre * value) + servo_centre;
		Assertions.assertEquals(pz_value, RangeUtil.map(value, -1, 1, 0, 180), DELTA_F);
		value = 0;
		pz_value = Math.round(servo_centre * value) + servo_centre;
		Assertions.assertEquals(pz_value, RangeUtil.map(value, -1, 1, 0, 180), DELTA_F);
		value = 1;
		pz_value = Math.round(servo_centre * value) + servo_centre;
		Assertions.assertEquals(pz_value, RangeUtil.map(value, -1, 1, 0, 180), DELTA_F);
	}

	@Test
	public void testConstrain() {
		int min = -90;
		int max = 90;
		Assertions.assertEquals(min, RangeUtil.constrain(min, min, max));
		Assertions.assertEquals(min, RangeUtil.constrain(min - 1, min, max));
		Assertions.assertEquals(max, RangeUtil.constrain(max, min, max));
		Assertions.assertEquals(max, RangeUtil.constrain(max + 1, min, max));
		Assertions.assertEquals(max / 2, RangeUtil.constrain(max / 2, min, max));
	}
	
	@Test
	public void testMax30102Map() {
		float min_current = 0f;
		float max_current = 51f;
		int min_val = 0;
		int max_val = 255;
		
		Assertions.assertEquals(0x00, RangeUtil.map(0.0f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x01, RangeUtil.map(0.2f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x02, RangeUtil.map(0.4f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x0f, RangeUtil.map(3.0f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x1f, RangeUtil.map(6.2f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x24, RangeUtil.map(7.1f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x3f, RangeUtil.map(12.6f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x7f, RangeUtil.map(25.4f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0xff, RangeUtil.map(51f, min_current, max_current, min_val, max_val));
	}
}
