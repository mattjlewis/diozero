package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.devices.sandpit.SevenSegmentDisplay;
import com.diozero.util.SleepUtil;

public class SevenSegmentDisplayTest {
	public static void main(String[] args) {
		try (SevenSegmentDisplay disp = new SevenSegmentDisplay(25, 23, 5, 6, 16, 24, 11,
				new int[] { 20, 21, 19, 26 })) {
			Logger.info("First only");
			boolean[] digits = new boolean[] { true, false, false, false };
			int delay_ms = 200;
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			Logger.info("All");
			digits = new boolean[] { true, true, true, true };
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			Logger.info("None");
			digits = new boolean[] { false, false, false, false };
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			Logger.info("Alternate");
			digits = new boolean[] { true, false, true, false };
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			Logger.info("Countdown");
			// Countdown from 9999
			int number = 9999;
			int decrement_delta_ms = 50;
			long last_change = System.currentTimeMillis();
			while (true) {
				for (int i = 0; i < 4; i++) {
					int digit = (number / (int) (Math.pow(10, 3 - i))) % 10;
					disp.enableDigit(i);
					disp.displayNumber(digit);
					SleepUtil.sleepMillis(5);
				}
				if (System.currentTimeMillis() - last_change > decrement_delta_ms) {
					number--;
					if (number < 0) {
						number = 9999;
					}
					last_change = System.currentTimeMillis();
				}
			}
		}
	}
}
