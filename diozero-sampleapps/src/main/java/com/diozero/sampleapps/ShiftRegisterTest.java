package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.devices.LED;
import com.diozero.devices.OutputShiftRegister;
import com.diozero.devices.PwmLed;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

public class ShiftRegisterTest {
	public static void main(String[] args) {
		// White wire (SER pin 14)
		int data_gpio = 22;
		// int data_gpio = 4;
		// Purple wire (SRCLK pin 11)
		int clock_gpio = 17;
		// int clock_gpio = 6;
		// Brown wire (RCLK pin 12)
		int latch_gpio = 27;
		// int latch_gpio = 5;

		int delay_ms = 500;

		try (OutputShiftRegister osr = new OutputShiftRegister(data_gpio, clock_gpio,
				latch_gpio, 8)) {
			for (int i = 0; i < 3; i++) {
				Logger.info("All on");
				for (int pin = 0; pin < 8; pin++) {
					osr.setBufferedValue(pin, true);
				}
				osr.flush();
				SleepUtil.sleepMillis(delay_ms);

				Logger.info("Alternate");
				for (int pin = 0; pin < 8; pin++) {
					osr.setBufferedValue(pin, (pin % 2) == 0);
				}
				osr.flush();
				SleepUtil.sleepMillis(delay_ms);

				Logger.info("Alternate opposite");
				for (int pin = 0; pin < 8; pin++) {
					osr.setBufferedValue(pin, (pin % 2) == 1);
				}
				osr.flush();
				SleepUtil.sleepMillis(delay_ms);

				Logger.info("One by one");
				for (int pin = 0; pin < 8; pin++) {
					Logger.info("pin {}", Integer.valueOf(pin));
					/*-
					for (int x = 0; x < 8; x++) {
						osr.setBufferedValue(x, pin == x);
					}
					osr.flush();
					*/
					osr.setValues(0, (byte) (1 << pin));
					SleepUtil.sleepMillis(delay_ms);
				}

				Logger.info("All off");
				for (int pin = 0; pin < 8; pin++) {
					osr.setBufferedValue(pin, false);
				}
				osr.flush();
				SleepUtil.sleepMillis(delay_ms);
			}

			for (int i = 1; i < 4; i++) {
				try (LED led = new LED(osr, i)) {
					Logger.info("LED {} on", Integer.valueOf(i));
					led.on();
					SleepUtil.sleepMillis(delay_ms);

					Logger.info("LED {} off", Integer.valueOf(i));
					led.off();
					SleepUtil.sleepMillis(delay_ms);

					Logger.info("LED {} blink", Integer.valueOf(i));
					led.blink(0.25f, 0.25f, 4, false);
				}
			}

			int pwm_num = 1;
			try (PwmLed pwm_led = new PwmLed(osr, pwm_num)) {
				for (float f = 0; f < 1; f += 0.05f) {
					Logger.info("PWM {} value {#,###.##}", Integer.valueOf(pwm_num), Float.valueOf(f));
					pwm_led.setValue(f);
					SleepUtil.sleepMillis(150);
				}
				for (float f = 1; f >= 0; f -= 0.05f) {
					Logger.info("PWM {} value {#,###.##}", Integer.valueOf(pwm_num), Float.valueOf(f));
					pwm_led.setValue(f);
					SleepUtil.sleepMillis(150);
				}
			}
		} finally {
			Diozero.shutdown();
		}
	}
}
