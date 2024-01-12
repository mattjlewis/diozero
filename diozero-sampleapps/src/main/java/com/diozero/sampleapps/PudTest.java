package com.diozero.sampleapps;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.SleepUtil;

public class PudTest {
	public static void main(String[] args) {
		System.out.println("PUD None");
		try (DigitalInputDevice did = DigitalInputDevice.Builder.builder(22).setPullUpDown(GpioPullUpDown.NONE)
				.build()) {
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
		}

		SleepUtil.sleepSeconds(0.5);
		System.out.println("PUD Pull Up");
		try (DigitalInputDevice did = DigitalInputDevice.Builder.builder(22).setPullUpDown(GpioPullUpDown.PULL_UP)
				.build()) {
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
		}

		SleepUtil.sleepSeconds(0.5);
		System.out.println("PUD Pull Down");
		try (DigitalInputDevice did = DigitalInputDevice.Builder.builder(22).setPullUpDown(GpioPullUpDown.PULL_DOWN)
				.build()) {
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
			SleepUtil.sleepSeconds(0.2);
			System.out.println(did.getValue());
		}
	}
}
