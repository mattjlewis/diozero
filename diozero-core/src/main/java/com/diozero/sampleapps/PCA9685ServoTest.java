package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import org.pmw.tinylog.Logger;

import com.diozero.PCA9685;
import com.diozero.sandpit.Servo;
import com.diozero.util.SleepUtil;

/**
 * 
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-jdkdio10-0.4-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-jdkdio11-0.4-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-pi4j-0.4-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-wiringpi-0.4-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-pigpio-0.4-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.PCA9685ServoTest 60 15
 */
public class PCA9685ServoTest {
	private static final float TOWERPRO_SG90_MIN_MS = 0.6f;
	private static final float TOWERPRO_SG90_MAX_MS = 2.5f;
	private static final float TOWERPRO_SG90_MID_MS = (TOWERPRO_SG90_MIN_MS + TOWERPRO_SG90_MAX_MS) / 2;
	
	public static void main(String[] args) {
		int pwm_freq = 50;
		int pin_number = 15;
		test(pwm_freq, pin_number);
		pin_number = 14;
		test(pwm_freq, pin_number);
	}
	
	public static void test(int pwmFrequency, int pinNumber) {
		try (PCA9685 pca9685 = new PCA9685(pwmFrequency);
				Servo servo = new Servo(pca9685, pinNumber, pwmFrequency, TOWERPRO_SG90_MID_MS)) {
			Logger.info("Mid");
			pca9685.setServoPulseWidthMs(pinNumber, TOWERPRO_SG90_MID_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Max");
			pca9685.setServoPulseWidthMs(pinNumber, TOWERPRO_SG90_MAX_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Mid");
			pca9685.setServoPulseWidthMs(pinNumber, TOWERPRO_SG90_MID_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Min");
			pca9685.setServoPulseWidthMs(pinNumber, TOWERPRO_SG90_MIN_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Mid");
			pca9685.setServoPulseWidthMs(pinNumber, TOWERPRO_SG90_MID_MS);
			SleepUtil.sleepMillis(1000);
			
			/*
			Logger.info("Mid");
			servo.setValue(TOWERPRO_SG90_MID_MS * pca9685.getPwmFrequency(pinNumber) / 1000f);
			SleepUtil.sleepMillis(1000);
			
			for (float i=TOWERPRO_SG90_MID_MS; i>TOWERPRO_SG90_MIN_MS; i-=0.01) {
				servo.setValue(i * pca9685.getPwmFrequency(pinNumber) / 1000f);
				SleepUtil.sleepMillis(10);
			}
			for (float i=TOWERPRO_SG90_MIN_MS; i<TOWERPRO_SG90_MAX_MS; i+=0.01) {
				servo.setValue(i * pca9685.getPwmFrequency(pinNumber) / 1000f);
				SleepUtil.sleepMillis(10);
			}
			for (float i=TOWERPRO_SG90_MAX_MS; i>TOWERPRO_SG90_MID_MS; i-=0.01) {
				servo.setValue(i * pca9685.getPwmFrequency(pinNumber) / 1000f);
				SleepUtil.sleepMillis(10);
			}
			*/
			
			for (float pulse_ms=TOWERPRO_SG90_MID_MS; pulse_ms<TOWERPRO_SG90_MAX_MS; pulse_ms+=0.01) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
			for (float pulse_ms=TOWERPRO_SG90_MAX_MS; pulse_ms>TOWERPRO_SG90_MIN_MS; pulse_ms-=0.01) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
			for (float pulse_ms=TOWERPRO_SG90_MIN_MS; pulse_ms<TOWERPRO_SG90_MID_MS; pulse_ms+=0.01) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
		}
	}
}
