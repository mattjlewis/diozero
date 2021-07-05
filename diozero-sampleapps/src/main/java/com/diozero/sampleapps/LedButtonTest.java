package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.api.RuntimeInterruptedException;
import com.diozero.devices.LedButton;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

public class LedButtonTest {
	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <button-gpio> <led-gpio>");
			return;
		}

		int button_gpio = Integer.parseInt(args[0]);
		int led_gpio = Integer.parseInt(args[1]);

		int sleep_sec = 20;

		try (LedButton led_button = new LedButton(button_gpio, led_gpio)) {
			Logger.info("Sleeping for {} seconds", Integer.valueOf(sleep_sec));
			SleepUtil.sleepSeconds(sleep_sec);
		} catch (RuntimeInterruptedException e) {
			// Ignore
		} finally {
			Diozero.shutdown();
		}
	}
}
