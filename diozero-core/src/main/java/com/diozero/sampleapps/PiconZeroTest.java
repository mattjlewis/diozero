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


import com.diozero.PiconZero;
import com.diozero.PiconZero.OutputConfig;
import com.diozero.util.SleepUtil;

public class PiconZeroTest {
	private static final int MOTOR = 1;
	private static final int SERVO1 = 0;
	private static final int SERVO2 = 1;
	private static final int LED = 4;
	private static final int PWM_MAX = 100;
	
	public static void main(String[] args) {
		try (PiconZero pz = new PiconZero()) {
			byte[] revision = pz.getRevision();
			System.out.format("Revision: Board %d, Firmware %d\n", Byte.valueOf(revision[0]), Byte.valueOf(revision[1]));
			
			for (int i=0; i<10; i++) {
				int val = i * 10;
				System.out.println("Setting motor output to " + val);
				pz.setMotor(MOTOR, val);
				SleepUtil.sleepSeconds(1);
			}
			
			pz.setMotor(MOTOR, 0);
			
			pz.setOutputConfig(SERVO1, PiconZero.OutputConfig.SERVO);
			pz.setOutputConfig(SERVO2, PiconZero.OutputConfig.SERVO);
			int SERVO_MID = 90;
			int MAX_DELTA = 70;
			int STEP=1;
			double DELAY = 0.02;
			for (int i=SERVO_MID; i<SERVO_MID+MAX_DELTA; i+=STEP) {
				System.out.println("Setting servo output to " + i);
				pz.setOutput(SERVO1, i);
				pz.setOutput(SERVO2, i);
				SleepUtil.sleepSeconds(DELAY);
			}
			for (int i=SERVO_MID+MAX_DELTA; i>SERVO_MID-MAX_DELTA; i-=STEP) {
				System.out.println("Setting servo output to " + i);
				pz.setOutput(SERVO1, i);
				pz.setOutput(SERVO2, i);
				SleepUtil.sleepSeconds(DELAY);
			}
			for (int i=SERVO_MID-MAX_DELTA; i<SERVO_MID; i+=STEP) {
				System.out.println("Setting servo output to " + i);
				pz.setOutput(SERVO1, i);
				pz.setOutput(SERVO2, i);
				SleepUtil.sleepSeconds(DELAY);
			}

			pz.setOutputConfig(LED, OutputConfig.PWM);
			for (int i=0; i<PWM_MAX; i++) {
				int val = i;
				System.out.println("Setting LED PWM value to " + val);
				pz.setOutput(LED, val);
			}
			for (int i=0; i<PWM_MAX; i++) {
				int val = PWM_MAX - i;
				System.out.println("Setting LED PWM value to " + val);
				pz.setOutput(LED, val);
			}
			
			pz.setOutputConfig(LED, OutputConfig.DIGITAL);
			boolean value = false;
			for (int i=0; i<10; i++) {
				System.out.println("Setting LED value to " + value);
				pz.setOutput(LED, value ? 1 : 0);
				value = ! value;
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
