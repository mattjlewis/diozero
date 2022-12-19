package com.diozero.sampleapps;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     ServoTest.java
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

import org.tinylog.Logger;

import com.diozero.animation.Animation;
import com.diozero.animation.AnimationInstance;
import com.diozero.animation.easing.Quintic;
import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

/**
 * Servo test application.
 */
public class ServoTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <pin number>", ServoTest.class.getName());
			System.exit(1);
		}

		int pin_number = Integer.parseInt(args[0]);

		test(pin_number);
	}

	public static void test(int gpio) {
		ServoTrim trim = ServoTrim.TOWERPRO_SG90;
		try (ServoDevice servo = ServoDevice.Builder.builder(gpio).setTrim(trim).build()) {
			for (int i = 0; i < 2; i++) {
				Logger.info("Mid: {#,###} us", Integer.valueOf(trim.getMidPulseWidthUs()));
				servo.setPulseWidthUs(trim.getMidPulseWidthUs());
				SleepUtil.sleepMillis(1000);

				Logger.info("Min: {#,###} us", Integer.valueOf(trim.getMinPulseWidthUs()));
				servo.setPulseWidthUs(trim.getMinPulseWidthUs());
				SleepUtil.sleepMillis(1000);

				Logger.info("Mid: {#,###} us", Integer.valueOf(trim.getMidPulseWidthUs()));
				servo.setPulseWidthUs(trim.getMidPulseWidthUs());
				SleepUtil.sleepMillis(1000);

				Logger.info("Max: {#,###} us", Integer.valueOf(trim.getMaxPulseWidthUs()));
				servo.setPulseWidthUs(trim.getMaxPulseWidthUs());
				SleepUtil.sleepMillis(1000);

				Logger.info("Mid: {#,###} us", Integer.valueOf(trim.getMidPulseWidthUs()));
				servo.setPulseWidthUs(trim.getMidPulseWidthUs());
				SleepUtil.sleepMillis(1000);

				Logger.info("From mid {#,###} us to max {#,###} us...", Integer.valueOf(trim.getMidPulseWidthUs()),
						Integer.valueOf(trim.getMaxPulseWidthUs()));
				for (int pulse_us = trim.getMidPulseWidthUs(); pulse_us < trim.getMaxPulseWidthUs(); pulse_us += 5) {
					servo.setPulseWidthUs(pulse_us);
					SleepUtil.sleepMillis(10);
				}
				Logger.info("From max {#,###} us to min {#,###} us...", Integer.valueOf(trim.getMaxPulseWidthUs()),
						Integer.valueOf(trim.getMinPulseWidthUs()));
				for (int pulse_us = trim.getMaxPulseWidthUs(); pulse_us > trim.getMinPulseWidthUs(); pulse_us -= 5) {
					servo.setPulseWidthUs(pulse_us);
					SleepUtil.sleepMillis(10);
				}
				Logger.info("From min {#,###} us to mid {#,###} us...", Integer.valueOf(trim.getMinPulseWidthUs()),
						Integer.valueOf(trim.getMidPulseWidthUs()));
				for (int pulse_us = trim.getMinPulseWidthUs(); pulse_us < trim.getMidPulseWidthUs(); pulse_us += 5) {
					servo.setPulseWidthUs(pulse_us);
					SleepUtil.sleepMillis(10);
				}

				Logger.info("From mid angle {} to max angle {}", Integer.valueOf(trim.getMidAngle()),
						Integer.valueOf(trim.getMaxAngle()));
				for (int angle = trim.getMidAngle(); angle < trim.getMaxAngle(); angle += 2) {
					servo.setAngle(angle);
					SleepUtil.sleepMillis(10);
				}
				Logger.info("From max angle {} to min angle {}", Integer.valueOf(trim.getMaxAngle()),
						Integer.valueOf(trim.getMinAngle()));
				for (int angle = trim.getMaxAngle(); angle > trim.getMinAngle(); angle -= 2) {
					servo.setAngle(angle);
					SleepUtil.sleepMillis(10);
				}
				Logger.info("From min angle {} to mid angle {}", Integer.valueOf(trim.getMinAngle()),
						Integer.valueOf(trim.getMidAngle()));
				for (int angle = trim.getMinAngle(); angle < trim.getMidAngle(); angle += 2) {
					servo.setAngle(angle);
					SleepUtil.sleepMillis(10);
				}

				Logger.info("Animation.");

				// 50 FPS, speed 1x, ease in-out cubic
				Animation animation = new Animation(servo::setAngle, 50, Quintic::easeInOut, 1f, false);

				// Relative time points (mid to max 25%, max to min 50%, min to max 50%)
				float[] cue_points = { 0, 0.25f, 0.75f, 1 };
				// Value for each target at each relative time point (mid, max, min, mid)
				List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame
						.fromValues(new float[][] { { trim.getMidAngle() }, { trim.getMaxAngle() },
								{ trim.getMinAngle() }, { trim.getMidAngle() } });

				AnimationInstance ai = new AnimationInstance(5_000, cue_points, key_frames);
				animation.enqueue(ai);

				Logger.info("Playing animation...");
				ScheduledFuture<?> future = animation.play();
				try {
					future.get();
				} catch (CancellationException e) {
					// Ignore
				}
				Logger.info("Animation finished");
			}
		} catch (InterruptedException | ExecutionException e) {
			Logger.error(e, "Error: {}", e);
		} finally {
			Diozero.shutdown();
		}
	}
}
