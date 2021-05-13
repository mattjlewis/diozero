package com.diozero.sampleapps;

import com.diozero.api.PwmOutputDevice;
import com.diozero.util.SleepUtil;

public class PwmOutputTest {
	public static void main(String[] args) {
		try (PwmOutputDevice pwm = new PwmOutputDevice(21)) {
			for (int i = 0; i < 5; i++) {
				System.out.println("50Hz");
				pwm.setPwmFrequency(50);
				for (float val = 0.1f; val <= 1; val += 0.1f) {
					System.out.println("Val: " + val);
					pwm.setValue(val);
					SleepUtil.sleepSeconds(1);
				}

				System.out.println("100Hz");
				pwm.setPwmFrequency(100);
				for (float val = 0.1f; val <= 1; val += 0.1f) {
					System.out.println("Val: " + val);
					pwm.setValue(val);
					SleepUtil.sleepSeconds(1);
				}

				float val = 0.5f;
				pwm.setValue(val);
				for (int x = 1; x < 100; x++) {
					System.out.println("PWM Freq: " + x + ", Val: " + val);
					pwm.setPwmFrequency(x);
					SleepUtil.sleepMillis(100);
				}
			}
		}
	}
}
