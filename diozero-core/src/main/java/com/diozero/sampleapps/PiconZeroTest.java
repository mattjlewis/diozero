package com.diozero.sampleapps;

import com.diozero.sandpit.PiconZero;
import com.diozero.sandpit.PiconZero.OutputConfig;
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
