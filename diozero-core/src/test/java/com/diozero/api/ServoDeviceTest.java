package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ServoDeviceTest.java
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

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.RangeUtil;
import com.diozero.util.ServoUtil;

@SuppressWarnings("static-method")
public class ServoDeviceTest {
	private static final int DIGITAL_GPIO = 10;
	private static final int PWM_GPIO = 12;
	private static final int SERVO_GPIO = 18;

	// 0..180 deg range, 0.6ms to 2.4ms, mid 1.5ms, 90deg move = 0.9ms
	private static final ServoTrim TRIM = ServoTrim.DEFAULT;

	private static final float FLOAT_COMPARISON_DELTA = 0.000001f;

	@BeforeAll
	public static void beforeAll() {
		final BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();

		board_info.addGpioPinInfo(DIGITAL_GPIO, DIGITAL_GPIO, EnumSet.of(DeviceMode.DIGITAL_OUTPUT));
		board_info.addGpioPinInfo(PWM_GPIO, PWM_GPIO, EnumSet.of(DeviceMode.PWM_OUTPUT));
		board_info.addGpioPinInfo(SERVO_GPIO, SERVO_GPIO, EnumSet.of(DeviceMode.SERVO));
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
		int pwm_freq = 50;
		int bits = 12;
		int range = (int) Math.pow(2, bits);
		double ms_per_bit = ServoUtil.calcPulseMsPerBit(pwm_freq, range);

		ServoTrim trim = ServoTrim.TOWERPRO_SG5010;

		float pulse_width_ms = trim.getMinPulseWidthMs();
		int servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG5010 Min: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(102, servo_pulse);

		pulse_width_ms = trim.getMaxPulseWidthMs();
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG5010 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(512, servo_pulse);

		trim = ServoTrim.TOWERPRO_SG90;

		pulse_width_ms = trim.getMinPulseWidthMs();
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Min: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(122, servo_pulse);

		pulse_width_ms = trim.getMaxPulseWidthMs();
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);
		Assertions.assertEquals(491, servo_pulse);

		// Error - value greater than range...
		pulse_width_ms = 40;
		servo_pulse = ServoUtil.calcServoPulseBits(pulse_width_ms, ms_per_bit);
		System.out.println("TowerPro SG90 Max: On time=" + pulse_width_ms + ", servo_pulse=" + servo_pulse);

		double max_pulse_width = 20;
		System.out.println("Max pulse width=" + (1000.0 / pwm_freq) + "ms");
		servo_pulse = ServoUtil.calcServoPulseBits(max_pulse_width, pwm_freq, range);
		Assertions.assertEquals(range, servo_pulse);

		float val = trim.getMinPulseWidthMs() * pwm_freq / 1000f;
		System.out.println("Pulse width of " + trim.getMinPulseWidthMs() + "ms is " + (val * 100f) + "%");
		Assertions.assertEquals(0.03f, val, FLOAT_COMPARISON_DELTA);

		pulse_width_ms = val * 1000f / pwm_freq;
		System.out.println("Val of " + (val * 100f) + "% is " + trim.getMinPulseWidthMs() + "ms");
		Assertions.assertEquals(trim.getMinPulseWidthMs(), pulse_width_ms, FLOAT_COMPARISON_DELTA);

		val = trim.getMaxPulseWidthMs() * pwm_freq / 1000f;
		System.out.println("Pulse width of " + trim.getMaxPulseWidthMs() + "ms is " + (val * 100f) + "%");
		Assertions.assertEquals(0.12f, val, FLOAT_COMPARISON_DELTA);

		pulse_width_ms = val * 1000f / pwm_freq;
		System.out.println("Val of " + (val * 100f) + "% is " + trim.getMaxPulseWidthMs() + "ms");
		Assertions.assertEquals(trim.getMaxPulseWidthMs(), pulse_width_ms, FLOAT_COMPARISON_DELTA);
	}

	@Test
	public void mg996rServoTest() {
		final ServoTrim trim = ServoTrim.MG996R;
		Logger.debug("Min Pulse Width: {#,###} us, Min Angle: {} degrees", Integer.valueOf(trim.getMinPulseWidthUs()),
				Integer.valueOf(trim.getMinAngle()));
		Logger.debug("Mid Pulse Width: {#,###} us, Mid Angle: {} degrees", Integer.valueOf(trim.getMidPulseWidthUs()),
				Integer.valueOf(trim.getMidAngle()));
		Logger.debug("Max Pulse Width: {#,###} us, Max Angle: {} degrees", Integer.valueOf(trim.getMaxPulseWidthUs()),
				Integer.valueOf(trim.getMaxAngle()));
		try (final ServoDevice servo = ServoDevice.Builder.builder(SERVO_GPIO).setTrim(trim).build()) {
			servo.setAngle(-90);
			Logger.debug("Pulse Width: {#,###} us, Angle: {}", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(trim.getMinAngle(), servo.getAngle());
			Assertions.assertEquals(trim.getMinPulseWidthUs(), servo.getPulseWidthUs());

			servo.setAngle(0);
			Logger.debug("Pulse Width: {#,###} us, Angle: {}", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(0, servo.getAngle());
			Assertions.assertEquals(trim.getMidPulseWidthUs() - trim.getNinetyDegPulseWidthUs(),
					servo.getPulseWidthUs());

			servo.setAngle(90);
			Logger.debug("Pulse Width: {#,###} us, Angle: {} degrees", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(90, servo.getAngle());
			Assertions.assertEquals(trim.getMidPulseWidthUs(), servo.getPulseWidthUs());

			servo.setAngle(180);
			Logger.debug("Pulse Width: {#,###} us, Angle: {} degrees", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(180, servo.getAngle());
			Assertions.assertEquals(trim.getMidPulseWidthUs() + trim.getNinetyDegPulseWidthUs(),
					servo.getPulseWidthUs());

			servo.setAngle(270);
			Logger.debug("Pulse Width: {#,###} us, Angle: {} degrees", Integer.valueOf(servo.getPulseWidthUs()),
					Float.valueOf(servo.getAngle()));
			Assertions.assertEquals(190, servo.getAngle());
			Assertions.assertEquals(trim.getMaxPulseWidthUs(), servo.getPulseWidthUs());

			runAngleTests(servo);
			runValueTests(servo);
		}
	}

	@Test
	public void testDigitalGpioServo() {
		try (final ServoDevice servo = ServoDevice.Builder.builder(DIGITAL_GPIO).setTrim(TRIM).build()) {
			runAngleTests(servo);
			runValueTests(servo);
		}
	}

	@Test
	public void testPwmServo() {
		for (int i = 0; i < 2; i++) {
			try (final ServoDevice servo = ServoDevice.Builder.builder(PWM_GPIO).setTrim(TRIM).build()) {
				runAngleTests(servo);
				runValueTests(servo);
			}
		}
	}

	@Test
	public void testServo() {
		try (final ServoDevice servo = ServoDevice.Builder.builder(SERVO_GPIO).setTrim(TRIM).build()) {
			runAngleTests(servo);
			runValueTests(servo);
		}
	}

	private static void runAngleTests(final ServoDevice servo) {
		final ServoTrim trim = servo.getTrim();

		System.out.println("runAngleTests start: " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());

		// Range is 0..180 so gets limited to min
		float angle = -90f;
		servo.setAngle(angle);
		Assertions.assertEquals(trim.getMinAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMinPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(-1, servo.getValue());

		// 0 degrees (approx min)
		angle = 0;
		servo.setAngle(angle);
		System.out.println("setAngle(" + angle + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(Math.max(angle, trim.getMinAngle()), servo.getAngle());
		Assertions.assertEquals(
				Math.max(trim.getMidPulseWidthUs() - trim.getNinetyDegPulseWidthUs(), trim.getMinPulseWidthUs()),
				servo.getPulseWidthUs());
		Assertions.assertEquals(RangeUtil.map(angle, trim.getMinAngle(), trim.getMaxAngle(), -1f, 1f),
				servo.getValue());

		// Mid
		angle = 90f;
		servo.setAngle(angle);
		System.out.println("setAngle(" + angle + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(angle, servo.getAngle());
		Assertions.assertEquals(trim.getMidPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(0, servo.getValue());

		// 180 degrees (approx max)
		angle = 180f;
		servo.setAngle(angle);
		System.out.println("setAngle(" + angle + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(angle, servo.getAngle());
		Assertions.assertEquals(
				Math.min(trim.getMidPulseWidthUs() + trim.getNinetyDegPulseWidthUs(), trim.getMaxPulseWidthUs()),
				servo.getPulseWidthUs());
		Assertions.assertEquals(RangeUtil.map(angle, trim.getMinAngle(), trim.getMaxAngle(), -1f, 1f), servo.getValue(),
				servo.getValue());

		// Range is approx 0..180 so gets limited to max
		angle = 270f;
		servo.setAngle(angle);
		System.out.println("setAngle(" + angle + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(trim.getMaxAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMaxPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(1, servo.getValue());
	}

	private static void runValueTests(final ServoDevice servo) {
		final ServoTrim trim = servo.getTrim();

		System.out.println("runValueTests start: " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());

		// Range is -1..1
		float value = -1.5f;
		servo.setValue(value);
		System.out.println("setValue(" + value + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(trim.getMinAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMinPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(-1, servo.getValue());

		// Fully left
		value = -1f;
		servo.setValue(value);
		System.out.println("setValue(" + value + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(trim.getMinAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMinPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(value, servo.getValue());

		// Mid
		value = 0;
		servo.setValue(value);
		System.out.println("setValue(" + value + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue());
		Assertions.assertEquals(trim.getMidAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMidPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(value, servo.getValue());

		// 50% to full right (approx 135 deg)
		value = 0.5f;
		servo.setValue(value);
		System.out.println("setValue(" + value + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(RangeUtil.map(value, -1f, 1f, (float) trim.getMinAngle(), (float) trim.getMaxAngle()),
				servo.getAngle());
		Assertions.assertEquals(
				RangeUtil.map(value, -1f, 1f, (float) trim.getMinPulseWidthUs(), (float) trim.getMaxPulseWidthUs()),
				servo.getPulseWidthUs());
		Assertions.assertEquals(value, servo.getValue());

		// Full right
		value = 1;
		servo.setValue(value);
		System.out.println("setValue(" + value + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(trim.getMaxAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMaxPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(value, servo.getValue());

		// Beyond right hand max
		value = 1.5f;
		servo.setValue(value);
		System.out.println("setValue(" + value + "): " + servo.getPulseWidthUs() + "us, value: " + servo.getValue()
				+ ", angle: " + servo.getAngle());
		Assertions.assertEquals(trim.getMaxAngle(), servo.getAngle());
		Assertions.assertEquals(trim.getMaxPulseWidthUs(), servo.getPulseWidthUs());
		Assertions.assertEquals(1, servo.getValue());
	}
}
