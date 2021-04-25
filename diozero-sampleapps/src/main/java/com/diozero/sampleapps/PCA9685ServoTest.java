package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     PCA9685ServoTest.java
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

import org.tinylog.Logger;

import com.diozero.devices.PCA9685;
import com.diozero.devices.Servo;
import com.diozero.util.SleepUtil;

/**
 * PCA9685 sample application. To run:
 * <ul>
 * <li>Built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.PCA9685ServoTest 50 15}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.PCA9685ServoTest 50 15}</li>
 * </ul>
 */
public class PCA9685ServoTest {
	private static final long LARGE_DELAY = 500;
	private static final long SHORT_DELAY = 10;

	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <pwm frequency> <gpio>", PCA9685ServoTest.class.getName());
			System.exit(1);
		}
		int pwm_freq = Integer.parseInt(args[0]);
		int pin_number = Integer.parseInt(args[1]);
		test(pwm_freq, pin_number);
	}

	public static void test(int pwmFrequency, int gpio) {
		Servo.Trim trim = Servo.Trim.MG996R;
		try (PCA9685 pca9685 = new PCA9685(pwmFrequency);
				Servo servo = new Servo(pca9685, gpio, trim.getMidPulseWidthMs(), pwmFrequency, trim)) {
			Logger.info("Mid");
			pca9685.setServoPulseWidthMs(gpio, trim.getMidPulseWidthMs());
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Max");
			pca9685.setServoPulseWidthMs(gpio, trim.getMaxPulseWidthMs());
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Mid");
			pca9685.setServoPulseWidthMs(gpio, trim.getMidPulseWidthMs());
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Min");
			pca9685.setServoPulseWidthMs(gpio, trim.getMinPulseWidthMs());
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Mid");
			pca9685.setServoPulseWidthMs(gpio, trim.getMidPulseWidthMs());
			SleepUtil.sleepMillis(LARGE_DELAY);

			Logger.info("Max");
			servo.max();
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Centre");
			servo.centre();
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Min");
			servo.min();
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("Centre");
			servo.centre();
			SleepUtil.sleepMillis(LARGE_DELAY);

			Logger.info("0");
			servo.setAngle(0);
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("90 (Centre)");
			servo.setAngle(90);
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("180");
			servo.setAngle(180);
			SleepUtil.sleepMillis(LARGE_DELAY);
			Logger.info("90 (Centre)");
			servo.setAngle(90);
			SleepUtil.sleepMillis(LARGE_DELAY);

			for (float pulse_ms = trim.getMidPulseWidthMs(); pulse_ms < trim.getMaxPulseWidthMs(); pulse_ms += 0.01) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(SHORT_DELAY);
			}
			for (float pulse_ms = trim.getMaxPulseWidthMs(); pulse_ms > trim.getMinPulseWidthMs(); pulse_ms -= 0.01) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(SHORT_DELAY);
			}
			for (float pulse_ms = trim.getMinPulseWidthMs(); pulse_ms < trim.getMidPulseWidthMs(); pulse_ms += 0.01) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(SHORT_DELAY);
			}
		}
	}
}
