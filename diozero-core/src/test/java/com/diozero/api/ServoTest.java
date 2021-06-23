package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ServoTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestPwmOutputDevice;
import com.diozero.internal.provider.test.TestServoDevice;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.ServoUtil;

@SuppressWarnings("static-method")
public class ServoTest {
	private static final double TOWERPRO_SG5010_MIN_MS = 1;
	private static final double TOWERPRO_SG5010_MAX_MS = 2;
	private static final double TOWERPRO_SG90_MIN_MS = 0.6;
	private static final double TOWERPRO_SG90_MAX_MS = 2.5;
	private static final int DELTA = 1;

	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setPwmOutputDeviceClass(TestPwmOutputDevice.class);
		TestDeviceFactory.setServoDeviceClass(TestServoDevice.class);

		BoardPinInfo pin_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo();
		pin_info.addGpioPinInfo(0, 0, EnumSet.of(DeviceMode.SERVO));
	}

	@Test
	public void testSoftwarePwm() {
		int pwm_freq = 53;
		int period_ms = 1000 / pwm_freq;
		System.out.println("Calculated period " + period_ms + " ms");

		period_ms = Math.round(1_000f / pwm_freq);
		System.out.println("Calculated period " + period_ms + " ms");
	}

	@Test
	public void testPulsewidthCalcs() {
		int pwm_freq = 60;
		int bits = 12;
		int range = (int) Math.pow(2, bits);
		double ms_per_bit = ServoUtil.calcPulseMsPerBit(pwm_freq, range);

		double pulse_width_ms = TOWERPRO_SG5010_MIN_MS;
		int servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG5010 Min: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(245, servo_pulse);

		pulse_width_ms = TOWERPRO_SG5010_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG5010 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(491, servo_pulse);

		pulse_width_ms = TOWERPRO_SG90_MIN_MS;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Min: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(147, servo_pulse);

		pulse_width_ms = TOWERPRO_SG90_MAX_MS;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(614, servo_pulse);

		// Error - value greater than range...
		pulse_width_ms = 20;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);

		double max_pulse_width = 16.666666667;
		servo_pulse = ServoUtil.calcServoPulseBits(max_pulse_width, pwm_freq, range);
		Assertions.assertEquals(range, servo_pulse);
		System.out.println(servo_pulse);

		System.out.println("Max pulse width=" + (1000.0 / pwm_freq) + "ms");

		double val = TOWERPRO_SG90_MIN_MS * pwm_freq / 1000f;
		System.out.println("Pulse width of " + TOWERPRO_SG90_MIN_MS + "ms is " + (val * 100f) + "%");
		Assertions.assertEquals(0.036, val, DELTA);

		pulse_width_ms = val * 1000f / pwm_freq;
		System.out.println("Val of " + (val * 100f) + "% is " + TOWERPRO_SG90_MIN_MS + "ms");
		Assertions.assertEquals(TOWERPRO_SG90_MIN_MS, pulse_width_ms, DELTA);
	}

	@Test
	public void servoTest() {
		ServoTrim trim = ServoTrim.MG996R;
		Logger.debug("Min Pulse Width: {#,###} us, Min Angle: {} degrees", Float.valueOf(trim.getMinPulseWidthUs()),
				Float.valueOf(trim.getMinAngle()));
		Logger.debug("Mid Pulse Width: {#,###} us, Mid Angle: {} degrees", Float.valueOf(trim.getMidPulseWidthUs()),
				Float.valueOf(trim.getMidAngle()));
		Logger.debug("Max Pulse Width: {#,###} us, Max Angle: {} degrees", Integer.valueOf(trim.getMaxPulseWidthUs()),
				Float.valueOf(trim.getMaxAngle()));
		try (ServoDevice servo = ServoDevice.newBuilder(0).setTrim(trim).build()) {
			servo.setAngle(0);
			Logger.debug("Pulse Width: {#,###} us, Angle: {}", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(trim.getMidPulseWidthUs() - trim.getNinetyDegPulseWidthUs(),
					servo.getPulseWidthUs(), DELTA);
			servo.setAngle(90);
			Logger.debug("Pulse Width: {#,###} us, Angle: {} degrees", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(trim.getMidPulseWidthUs(), servo.getPulseWidthUs(), DELTA);
			servo.setAngle(180);
			Logger.debug("Pulse Width: {#,###} us, Angle: {} degrees", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(trim.getMidPulseWidthUs() + trim.getNinetyDegPulseWidthUs(),
					servo.getPulseWidthUs(), DELTA);
		}
	}
}
