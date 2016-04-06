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

import com.diozero.sandpit.Servo;
import com.diozero.util.SleepUtil;

/**
 * Servo test application. To run:
 * <ul>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.ServoTest 50 13}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.ServoTest 50 13}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.ServoTest 50 13}</li>
 * </ul>
 */
public class ServoTest {
	private static final float TOWERPRO_SG90_MIN_MS = 0.6f;
	private static final float TOWERPRO_SG90_MAX_MS = 2.5f;
	private static final float TOWERPRO_SG90_MID_MS = (TOWERPRO_SG90_MIN_MS + TOWERPRO_SG90_MAX_MS) / 2;
	
	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <PWM Frequency> <BCM pin number>", ServoTest.class.getName());
			System.exit(1);
		}
		
		int pwm_freq = Integer.parseInt(args[0]);
		int pin_number = Integer.parseInt(args[1]);
		
		test(pwm_freq, pin_number);
	}
	
	public static void test(int frequency, int pinNumber) {
		try (Servo servo = new Servo(pinNumber, frequency, TOWERPRO_SG90_MID_MS)) {
			for (float pulse_ms=TOWERPRO_SG90_MID_MS; pulse_ms<TOWERPRO_SG90_MAX_MS; pulse_ms+=0.005) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
			for (float pulse_ms=TOWERPRO_SG90_MAX_MS; pulse_ms>TOWERPRO_SG90_MIN_MS; pulse_ms-=0.005) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
			for (float pulse_ms=TOWERPRO_SG90_MIN_MS; pulse_ms<TOWERPRO_SG90_MID_MS; pulse_ms+=0.005) {
				servo.setPulseWidthMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
		}
	}
}
