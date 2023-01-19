package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     PCA9685ServoAnimation.java
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.animation.Animation;
import com.diozero.animation.AnimationInstance;
import com.diozero.animation.easing.Sine;
import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.devices.PCA9685;

/**
 * PCA9685 sample application. To run:
 * <ul>
 * <li>Built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * </ul>
 */
public class PCA9685ServoAnimation {
	public static void main(String[] args) {
		int pwm_freq = 50;
		test(pwm_freq, 0, 1, 12, 13, 14, 15);
	}

	public static void test(int pwmFrequency, int gpio1, int gpio2, int gpio3, int gpio4, int gpio5, int gpio6) {
		ServoTrim trim = ServoTrim.TOWERPRO_SG90;
		try (PCA9685 pca9685 = new PCA9685(pwmFrequency);
				ServoDevice servo1 = ServoDevice.Builder.builder(gpio1).setDeviceFactory(pca9685).setTrim(trim).build();
				ServoDevice servo2 = ServoDevice.Builder.builder(gpio2).setDeviceFactory(pca9685).setTrim(trim).build();
				ServoDevice servo3 = ServoDevice.Builder.builder(gpio3).setDeviceFactory(pca9685).setTrim(trim).build();
				ServoDevice servo4 = ServoDevice.Builder.builder(gpio4).setDeviceFactory(pca9685).setTrim(trim).build();
				ServoDevice servo5 = ServoDevice.Builder.builder(gpio5).setDeviceFactory(pca9685).setTrim(trim).build();
				ServoDevice servo6 = ServoDevice.Builder.builder(gpio6).setDeviceFactory(pca9685).setTrim(trim)
						.build()) {
			Animation animation = new Animation(Arrays.asList(servo1::setAngle, servo2::setAngle, servo3::setAngle,
					servo4::setAngle, servo5::setAngle, servo6::setAngle), 100, Sine::easeIn, 1f, true);
			float[] cue_points = { 0, 0.2f, 0.5f, 1 };
			List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.fromValues(new float[][] {
					{ trim.getMinAngle(), trim.getMinAngle(), trim.getMinAngle(), trim.getMinAngle(),
							trim.getMinAngle(), trim.getMinAngle() },
					{ trim.getMidAngle(), trim.getMidAngle(), trim.getMidAngle(), trim.getMidAngle(),
							trim.getMidAngle(), trim.getMidAngle() },
					{ trim.getMaxAngle(), trim.getMaxAngle(), trim.getMaxAngle(), trim.getMaxAngle(),
							trim.getMaxAngle(), trim.getMaxAngle() },
					{ trim.getMinAngle(), trim.getMinAngle(), trim.getMinAngle(), trim.getMinAngle(),
							trim.getMinAngle(), trim.getMinAngle() } });
			AnimationInstance ai = new AnimationInstance(5000, cue_points, key_frames);
			animation.enqueue(ai);
			Future<?> future = animation.play();
			future.get();
		} catch (CancellationException e) {
			Logger.info("Cancelled");
		} catch (InterruptedException | ExecutionException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
