package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     PiconZeroTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import com.diozero.devices.PiconZero;
import com.diozero.devices.PiconZero.OutputConfig;
import com.diozero.util.RangeUtil;
import com.diozero.util.SleepUtil;

public class PiconZeroTest {
	private static final int MOTOR = 1;
	private static final int STEERING_SERVO = 0;
	private static final int TEST_SERVO = 1;
	private static final int GREEN_LED = 3;
	private static final int RED_LED = 4;
	private static final int PWM_MAX = 100;

	public static void main(String[] args) {
		try (PiconZero pz = new PiconZero()) {
			byte[] revision = pz.getRevision();
			System.out.format("Revision: Board %d, Firmware %d\n", Byte.valueOf(revision[0]),
					Byte.valueOf(revision[1]));

			/*-
			for (float f = 0; f < 1; f += 0.1) {
				System.out.println("Setting motor output to " + f);
				pz.setMotor(MOTOR, f);
				SleepUtil.sleepSeconds(1);
			}
			pz.setMotor(MOTOR, 0);
			*/

			pz.setOutputConfig(STEERING_SERVO, PiconZero.OutputConfig.SERVO);
			pz.setOutputConfig(TEST_SERVO, PiconZero.OutputConfig.SERVO);
			pz.setOutputConfig(GREEN_LED, OutputConfig.PWM);
			pz.setOutputConfig(RED_LED, OutputConfig.PWM);
			int SERVO_MID = 90;
			int MAX_DELTA = 70;
			int STEP = 1;
			double DELAY = 0.02;
			for (int i = SERVO_MID; i < SERVO_MID + MAX_DELTA; i += STEP) {
				System.out.println("Setting servo output to " + i);
				pz.setOutputValue(STEERING_SERVO, i);
				pz.setOutputValue(TEST_SERVO, i);
				pz.setOutputValue(GREEN_LED, RangeUtil.map(i, SERVO_MID - MAX_DELTA, SERVO_MID + MAX_DELTA, 0, 100));
				pz.setOutputValue(RED_LED, RangeUtil.map(i, SERVO_MID - MAX_DELTA, SERVO_MID + MAX_DELTA, 0, 100));
				SleepUtil.sleepSeconds(DELAY);
			}
			for (int i = SERVO_MID + MAX_DELTA; i > SERVO_MID - MAX_DELTA; i -= STEP) {
				System.out.println("Setting servo output to " + i);
				pz.setOutputValue(STEERING_SERVO, i);
				pz.setOutputValue(TEST_SERVO, i);
				pz.setOutputValue(GREEN_LED, RangeUtil.map(i, SERVO_MID - MAX_DELTA, SERVO_MID + MAX_DELTA, 0, 100));
				pz.setOutputValue(RED_LED, RangeUtil.map(i, SERVO_MID - MAX_DELTA, SERVO_MID + MAX_DELTA, 0, 100));
				SleepUtil.sleepSeconds(DELAY);
			}
			for (int i = SERVO_MID - MAX_DELTA; i < SERVO_MID; i += STEP) {
				System.out.println("Setting servo output to " + i);
				pz.setOutputValue(STEERING_SERVO, i);
				pz.setOutputValue(TEST_SERVO, i);
				pz.setOutputValue(GREEN_LED, RangeUtil.map(i, SERVO_MID - MAX_DELTA, SERVO_MID + MAX_DELTA, 0, 100));
				pz.setOutputValue(RED_LED, RangeUtil.map(i, SERVO_MID - MAX_DELTA, SERVO_MID + MAX_DELTA, 0, 100));
				SleepUtil.sleepSeconds(DELAY);
			}

			pz.setOutputConfig(GREEN_LED, OutputConfig.PWM);
			pz.setOutputConfig(RED_LED, OutputConfig.PWM);
			for (int i = 0; i < PWM_MAX; i++) {
				int val = i;
				System.out.println("Setting LED PWM value to " + val);
				pz.setOutputValue(GREEN_LED, val);
				pz.setOutputValue(RED_LED, val);
			}
			for (int i = 0; i < PWM_MAX; i++) {
				int val = PWM_MAX - i;
				System.out.println("Setting LED PWM value to " + val);
				pz.setOutputValue(GREEN_LED, val);
				pz.setOutputValue(RED_LED, val);
			}

			pz.setOutputConfig(GREEN_LED, OutputConfig.DIGITAL);
			pz.setOutputConfig(RED_LED, OutputConfig.DIGITAL);
			boolean value = false;
			for (int i = 0; i < 10; i++) {
				System.out.println("Setting LED value to " + value);
				pz.setOutputValue(GREEN_LED, value ? 1 : 0);
				pz.setOutputValue(RED_LED, value ? 1 : 0);
				value = !value;
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
