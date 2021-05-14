package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.devices.Button;
import com.diozero.devices.LED;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

public class ShutdownRestartTest {
	public static void main(String[] args) {
		for (int i = 0; i < 5; i++) {
			Logger.info("Loop #{}", Integer.valueOf(i));
			try (LED led = new LED(21); Button button = new Button(5)) {
				for (int x = 0; x < 5; x++) {
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
