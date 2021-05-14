package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.devices.Button;
import com.diozero.devices.LED;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

public class ShutdownRestartTest {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: " + ShutdownRestartTest.class.getName() + " <out-gpio> <in-gpio>");
			return;
		}
		int out_gpio = Integer.parseInt(args[0]);
		int in_gpio = Integer.parseInt(args[1]);

		for (int i = 0; i < 2; i++) {
			Logger.info("Loop #{}", Integer.valueOf(i));
			try (LED led = new LED(out_gpio); Button button = new Button(in_gpio)) {
				for (int x = 0; x < 2; x++) {
					led.on();
					SleepUtil.sleepMillis(500);
					led.off();
					SleepUtil.sleepMillis(500);
				}
			}

			SleepUtil.sleepMillis(500);
		}

		for (int i = 0; i < 2; i++) {
			Logger.info("Loop #{}", Integer.valueOf(i));
			try (LED led = new LED(out_gpio); Button button = new Button(in_gpio)) {
				for (int x = 0; x < 2; x++) {
					led.on();
					SleepUtil.sleepMillis(500);
					led.off();
					SleepUtil.sleepMillis(500);
				}
			} finally {
				DeviceFactoryHelper.shutdown();
			}

			SleepUtil.sleepMillis(500);
		}
	}
}
