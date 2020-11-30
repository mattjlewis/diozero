package com.diozero.sampleapps;

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads1x15;
import com.diozero.devices.Ads1x15.Ads1115DataRate;
import com.diozero.devices.Ads1x15.PgaConfig;
import com.diozero.util.SleepUtil;

public class Ads1115Test {
	public static void main(String[] args) {
		try (Ads1x15 adc = new Ads1x15(PgaConfig.PGA_4096MV, Ads1115DataRate.DR_860HZ);
				AnalogInputDevice ain = new AnalogInputDevice(adc, 3)) {
			System.out.println("Range: " + ain.getRange());
			for (int i = 0; i < 10; i++) {
				System.out.println("Raw: " + adc.getValue(ain.getGpio()));
				System.out.println("Unscaled: " + ain.getUnscaledValue() + "%; Scaled: " + ain.getScaledValue() + "v");
				SleepUtil.sleepMillis(500);
			}
		}

		try (Ads1x15 adc = new Ads1x15(PgaConfig.PGA_4096MV, Ads1115DataRate.DR_8HZ);
				AnalogInputDevice ain = new AnalogInputDevice(adc, 3);
				DigitalInputDevice ready_pin = new DigitalInputDevice(24, GpioPullUpDown.PULL_UP,
						GpioEventTrigger.BOTH)) {
			System.out.println("Range: " + ain.getRange());
			adc.setContinousMode(ready_pin, ain.getGpio());
			for (int i = 0; i < 10; i++) {
				System.out.println("Raw: " + adc.getValue(ain.getGpio()));
				System.out.println("Unscaled: " + ain.getUnscaledValue() + "%; Scaled: " + ain.getScaledValue() + "v");
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
