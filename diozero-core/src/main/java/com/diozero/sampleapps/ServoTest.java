package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.sandpit.Servo;
import com.diozero.util.SleepUtil;

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
