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

import static com.diozero.util.RangeUtil.constrain;
import static com.diozero.util.RangeUtil.map;
import static com.diozero.util.RangeUtil.resolutionEndExclusive;
import static com.diozero.util.RangeUtil.resolutionEndInclusive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diozero.api.ServoTrim;

@SuppressWarnings("static-method")
public class RangeUtilTest {
	private static final float DELTA_F = 0.001f;

	@Test
	public void resolutionTest() {
		final int _12bit_end_exclusive = resolutionEndExclusive(12);
		final int _12bit_end_inclusive = resolutionEndInclusive(12);

		Assertions.assertEquals(4095, _12bit_end_exclusive);
		Assertions.assertEquals(4096, _12bit_end_inclusive);

		Assertions.assertEquals(1f, map(_12bit_end_inclusive, _12bit_end_inclusive, 1f));
		Assertions.assertEquals(4095, map(1, 1f, _12bit_end_exclusive));
		Assertions.assertEquals(1, map(_12bit_end_exclusive, _12bit_end_exclusive, 1));
		Assertions.assertEquals(4095, map(1, 1, _12bit_end_exclusive));

		final int MAX_VALUE = _12bit_end_exclusive;
		final int[] on_off = { 0, MAX_VALUE };
		Assertions.assertEquals(1f, map(on_off[1] - on_off[0], MAX_VALUE, 1));
		Assertions.assertEquals(1f, (on_off[1] - on_off[0]) / (float) MAX_VALUE);
		on_off[0] = MAX_VALUE / 2;
		Assertions.assertEquals(0.5f, map(on_off[1] - on_off[0], MAX_VALUE, 1f), DELTA_F);
		Assertions.assertEquals(0.5f, (on_off[1] - on_off[0]) / (float) MAX_VALUE, DELTA_F);

		float value = 0.5f;
		// 2048 and not 2047 as 4095/2 gets rounded up
		Assertions.assertEquals(2048, Math.round(value * MAX_VALUE));
		Assertions.assertEquals(2048, map(value, 1, MAX_VALUE));
		value = 1;
		Assertions.assertEquals(MAX_VALUE, Math.round(value * MAX_VALUE));
		Assertions.assertEquals(MAX_VALUE, map(value, 1, MAX_VALUE));
		value = 0;
		Assertions.assertEquals(0, Math.round(value * MAX_VALUE));
		Assertions.assertEquals(0, map(value, 1, MAX_VALUE));

		final int CLOCK_FREQ = 25_000_000;
		final int DEFAULT_PWM_FREQUENCY = 50;
		int pwmFrequency = DEFAULT_PWM_FREQUENCY;
		double prescale_flt = (((float) CLOCK_FREQ) / _12bit_end_exclusive / pwmFrequency) - 1;
		System.out.println(prescale_flt);
		prescale_flt = (((float) CLOCK_FREQ) / _12bit_end_inclusive / pwmFrequency) - 1;
		System.out.println();

		double periodUs = 1_000_000.0 / DEFAULT_PWM_FREQUENCY;
		System.out.println(periodUs);
		ServoTrim trim = ServoTrim.DEFAULT;
		int off = (int) Math.round((trim.getMidPulseWidthUs() / periodUs) * MAX_VALUE);
		int off_map = (int) Math.round(map(trim.getMidPulseWidthUs(), periodUs, MAX_VALUE));
		System.out.println("Mid: " + off + ", " + off_map);

		off = (int) Math.round((trim.getMaxPulseWidthUs() / periodUs) * MAX_VALUE);
		off_map = (int) Math.round(map(trim.getMaxPulseWidthUs(), periodUs, MAX_VALUE));
		System.out.println("Max: " + off + ", " + off_map);

		off = (int) Math.round((trim.getMinPulseWidthUs() / periodUs) * MAX_VALUE);
		off_map = (int) Math.round(map(trim.getMinPulseWidthUs(), periodUs, MAX_VALUE));
		System.out.println("Min: " + off + ", " + off_map);
	}

	@Test
	public void testMapFloat() {
		float from_low = 0;
		float from_high = 1;
		int to_low_int = 0;
		int to_high_int = 180;

		Assertions.assertEquals(to_low_int, map(from_low, from_low, from_high, to_low_int, to_high_int));
		Assertions.assertEquals((to_low_int + to_high_int) / 2,
				map((from_low + from_high) / 2, from_low, from_high, to_low_int, to_high_int));
		Assertions.assertEquals(to_high_int, map(from_high, from_low, from_high, to_low_int, to_high_int));
		Assertions.assertEquals(to_low_int, map(from_low - 10, from_low, from_high, to_low_int, to_high_int), DELTA_F);
		Assertions.assertEquals(to_high_int, map(from_high + 10, from_low, from_high, to_low_int, to_high_int),
				DELTA_F);
		Assertions.assertEquals(-to_high_int,
				map(from_low - from_high, from_low, from_high, to_low_int, to_high_int, false), DELTA_F);
		Assertions.assertEquals(2 * to_high_int,
				map(2 * from_high, from_low, from_high, to_low_int, to_high_int, false), DELTA_F);

		from_low = -90;
		from_high = 90;
		float to_low_float = 1f;
		float to_high_float = 2f;
		Assertions.assertEquals(to_low_float, map(from_low, from_low, from_high, to_low_float, to_high_float), DELTA_F);
		Assertions.assertEquals((to_low_float + to_high_float) / 2,
				map((from_low + from_high) / 2, from_low, from_high, to_low_float, to_high_float), DELTA_F);
		Assertions.assertEquals(to_high_float, map(from_high, from_low, from_high, to_low_float, to_high_float),
				DELTA_F);
		Assertions.assertEquals(to_low_float, map(from_low - 10, from_low, from_high, to_low_float, to_high_float),
				DELTA_F);
		Assertions.assertEquals(to_high_float, map(from_high + 10, from_low, from_high, to_low_float, to_high_float),
				DELTA_F);
		Assertions.assertEquals(0.5, map(from_low - from_high, from_low, from_high, to_low_float, to_high_float, false),
				DELTA_F);
		Assertions.assertEquals(2.5, map(2 * from_high, from_low, from_high, to_low_float, to_high_float, false),
				DELTA_F);
	}

	@Test
	public void testMapInt() {
		Assertions.assertEquals(128, map(500, 1000, 256));
		Assertions.assertEquals(384, map(1500, 1000, 256, false));
		Assertions.assertEquals(128, map(0, -1000, 1000, 0, 256));
		Assertions.assertEquals(128, map(0.5f, 1f, 256f));
		Assertions.assertEquals(128, map(1.5f, 1f, 2f, 0, 256));
		Assertions.assertEquals(-128, map(0.5f, 1f, 2f, 0, 256, false));
		Assertions.assertEquals(128, map(0.5f, 1f, 256f));
		Assertions.assertEquals(128, map(1.5f, 1f, 2f, 0f, 256f));

		int from_high = 90;
		int to_high = 180;

		Assertions.assertEquals(0, map(0, from_high, to_high));
		Assertions.assertEquals(to_high / 2, map(from_high / 2, from_high, to_high));
		Assertions.assertEquals(to_high, map(from_high, from_high, to_high));
		Assertions.assertEquals(0, map(-10, from_high, to_high));
		Assertions.assertEquals(to_high, map(from_high + 10, from_high, to_high));
		Assertions.assertEquals(-to_high, map(-from_high, from_high, to_high, false));
		Assertions.assertEquals(2 * to_high, map(2 * from_high, from_high, to_high, false));

		int from_low = 0;
		from_high = 90;
		int to_low = 0;
		to_high = 180;

		Assertions.assertEquals(to_low, map(from_low, from_low, from_high, to_low, to_high));
		Assertions.assertEquals((to_low + to_high) / 2,
				map((from_low + from_high) / 2, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(to_high, map(from_high, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(to_low, map(from_low - 10, from_low, from_high, to_low, to_high), DELTA_F);
		Assertions.assertEquals(to_high, map(from_high + 10, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(-to_high, map(from_low - from_high, from_low, from_high, to_low, to_high, false));
		Assertions.assertEquals(2 * to_high, map(2 * from_high, from_low, from_high, to_low, to_high, false));

		from_low = -90;
		from_high = 90;
		to_low = 1000;
		to_high = 2000;
		Assertions.assertEquals(to_low, map(from_low, from_low, from_high, to_low, to_high), DELTA_F);
		Assertions.assertEquals((to_low + to_high) / 2,
				map((from_low + from_high) / 2, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(to_high, map(from_high, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(to_low, map(from_low - 10, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(to_high, map(from_high + 10, from_low, from_high, to_low, to_high));
		Assertions.assertEquals(500, map(from_low - from_high, from_low, from_high, to_low, to_high, false));
		Assertions.assertEquals(2500, map(2 * from_high, from_low, from_high, to_low, to_high, false));
	}

	@Test
	public void testServoRange() {
		float min_pulse_width_ms = 0.75f;
		float max_pulse_width_ms = 2.25f;
		float minus90_pulse_width_ms = 1f;
		float plus90_pulse_width_ms = 2f;

		Assertions.assertEquals(map(min_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false),
				-45);
		Assertions.assertEquals(map(max_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false),
				225);
		Assertions.assertEquals(
				map(minus90_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false), 0);
		Assertions.assertEquals(
				map(plus90_pulse_width_ms, minus90_pulse_width_ms, plus90_pulse_width_ms, 0, 180, false), 180);
		Assertions.assertEquals(map((minus90_pulse_width_ms + plus90_pulse_width_ms) / 2, minus90_pulse_width_ms,
				plus90_pulse_width_ms, 0, 180, false), 90);

		Assertions.assertEquals(0, map(-1, -1, 1, 0, 180));
		Assertions.assertEquals(90, map(0, -1, 1, 0, 180));
		Assertions.assertEquals(180, map(1, -1, 1, 0, 180));

		int servo_centre = 90;
		float value = -1;
		float pz_value = Math.round(servo_centre * value) + servo_centre;
		Assertions.assertEquals(pz_value, map(value, -1, 1, 0, 180), DELTA_F);
		value = 0;
		pz_value = Math.round(servo_centre * value) + servo_centre;
		Assertions.assertEquals(pz_value, map(value, -1, 1, 0, 180), DELTA_F);
		value = 1;
		pz_value = Math.round(servo_centre * value) + servo_centre;
		Assertions.assertEquals(pz_value, map(value, -1, 1, 0, 180), DELTA_F);
	}

	@Test
	public void testConstrain() {
		int min = -90;
		int max = 90;
		Assertions.assertEquals(min, constrain(min, min, max));
		Assertions.assertEquals(min, constrain(min - 1, min, max));
		Assertions.assertEquals(max, constrain(max, min, max));
		Assertions.assertEquals(max, constrain(max + 1, min, max));
		Assertions.assertEquals(max / 2, constrain(max / 2, min, max));
	}

	@Test
	public void testMax30102Map() {
		float min_current = 0f;
		float max_current = 51f;
		int min_val = 0;
		int max_val = 255;

		Assertions.assertEquals(0x00, map(0.0f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x01, map(0.2f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x02, map(0.4f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x0f, map(3.0f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x1f, map(6.2f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x24, map(7.1f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x3f, map(12.6f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0x7f, map(25.4f, min_current, max_current, min_val, max_val));
		Assertions.assertEquals(0xff, map(51f, min_current, max_current, min_val, max_val));
	}
}
