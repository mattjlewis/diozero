package com.diozero.sampleapps.sandpit;

import org.tinylog.Logger;

import com.diozero.devices.LED;
import com.diozero.devices.sandpit.OutputShiftRegisterDeviceFactory;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

public class ShiftRegisterTest {
	public static void main(String[] args) {
		// Purple wire (SER)
		// int data_gpio = 21;
		int data_gpio = 4;
		// Yellow wire (SRCLK)
		// int clock_gpio = 16;
		int clock_gpio = 6;
		// Green wire (RCLK)
		// int latch_gpio = 20;
		int latch_gpio = 5;

		try (OutputShiftRegisterDeviceFactory osr = new OutputShiftRegisterDeviceFactory(data_gpio, clock_gpio,
				latch_gpio, 8)) {
			for (int i = 0; i < 3; i++) {
				Logger.info("All on");
				for (int pin = 0; pin < 8; pin++) {
					osr.set(pin, true);
				}
				osr.flush();
				SleepUtil.sleepMillis(500);

				Logger.info("Alternate");
				for (int pin = 0; pin < 8; pin++) {
					osr.set(pin, (pin % 2) == 0);
				}
				osr.flush();
				SleepUtil.sleepMillis(500);

				Logger.info("Alternate opposite");
				for (int pin = 0; pin < 8; pin++) {
					osr.set(pin, (pin % 2) == 1);
				}
				osr.flush();
				SleepUtil.sleepMillis(500);

				Logger.info("One by one");
				for (int pin = 0; pin < 8; pin++) {
					Logger.info("pin {}", Integer.valueOf(pin));
					for (int x = 0; x < 8; x++) {
						osr.set(x, pin == x);
					}
					osr.flush();
					SleepUtil.sleepMillis(1000);
				}

				Logger.info("All off");
				for (int pin = 0; pin < 8; pin++) {
					osr.set(pin, false);
				}
				osr.flush();
				SleepUtil.sleepMillis(500);
			}

			for (int i = 1; i < 5; i++) {
				try (LED led = new LED(osr, i)) {
					Logger.info("LED {} on", Integer.valueOf(i));
					led.on();
					SleepUtil.sleepMillis(500);

					Logger.info("LED {} off", Integer.valueOf(i));
					led.off();
					SleepUtil.sleepMillis(500);

					Logger.info("LED {} blink", Integer.valueOf(i));
					led.blink(0.5f, 0.5f, 5, false);
				}
			}
		} finally {
			Diozero.shutdown();
		}
	}
}
