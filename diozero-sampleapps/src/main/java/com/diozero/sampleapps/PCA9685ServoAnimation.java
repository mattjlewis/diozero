package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Sample applications
 * Filename:     PCA9685ServoAnimation.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.Animation;
import com.diozero.api.AnimationInstance;
import com.diozero.api.easing.Sine;
import com.diozero.devices.PCA9685;
import com.diozero.devices.Servo;

/**
 * PCA9685 sample application. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * </ul>
 */
public class PCA9685ServoAnimation {
	public static void main(String[] args) {
		int pwm_freq = 50;
		test(pwm_freq, 0, 1, 12, 13, 14, 15);
	}
	
	public static void test(int pwmFrequency, int gpio1, int gpio2, int gpio3, int gpio4, int gpio5, int gpio6) {
		Servo.Trim trim = Servo.Trim.TOWERPRO_SG90;
		try (PCA9685 pca9685 = new PCA9685(pwmFrequency);
				Servo servo1 = new Servo(pca9685, gpio1, trim.getMidPulseWidthMs(), pwmFrequency, trim);
				Servo servo2 = new Servo(pca9685, gpio2, trim.getMidPulseWidthMs(), pwmFrequency, trim);
				Servo servo3 = new Servo(pca9685, gpio3, trim.getMidPulseWidthMs(), pwmFrequency, trim);
				Servo servo4 = new Servo(pca9685, gpio4, trim.getMidPulseWidthMs(), pwmFrequency, trim);
				Servo servo5 = new Servo(pca9685, gpio5, trim.getMidPulseWidthMs(), pwmFrequency, trim);
				Servo servo6 = new Servo(pca9685, gpio6, trim.getMidPulseWidthMs(), pwmFrequency, trim)) {
			Animation animation = new Animation(Arrays.asList(servo1::setAngle, servo2, servo3, servo4, servo5, servo6), 100,
					Sine::easeIn, 1f);
			animation.setLoop(true);
			float[] cue_points = { 0, 0.2f, 0.5f, 1 };
			List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.fromValues(
					new float[][] {
						{ trim.getMinAngle(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs() },
						{ trim.getMidAngle(), trim.getMidPulseWidthMs(), trim.getMidPulseWidthMs(), trim.getMidPulseWidthMs(), trim.getMidPulseWidthMs(), trim.getMidPulseWidthMs() },
						{ trim.getMaxAngle(), trim.getMaxPulseWidthMs(), trim.getMaxPulseWidthMs(), trim.getMaxPulseWidthMs(), trim.getMaxPulseWidthMs(), trim.getMaxPulseWidthMs() },
						{ trim.getMinAngle(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs(), trim.getMinPulseWidthMs() } } );
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
