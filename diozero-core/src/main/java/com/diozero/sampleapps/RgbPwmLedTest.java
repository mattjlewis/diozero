package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.RgbPwmLed;
import com.diozero.util.SleepUtil;

public class RgbPwmLedTest {
	public static void main(String[] args) {
		test(17, 27, 22);
	}
	
	private static void test(int redPin, int greenPin, int bluePin) {
		int delay = 500;
		try (RgbPwmLed led = new RgbPwmLed(redPin, greenPin, bluePin)) {
			Logger.info("Blue");
			led.setValues(0, 0, 1);	// 001
			SleepUtil.sleepMillis(delay);
			Logger.info("Green");
			led.setValues(0, 1, 0);	// 010
			SleepUtil.sleepMillis(delay);
			Logger.info("Blue + Green");
			led.setValues(0, 1, 1);	// 011
			SleepUtil.sleepMillis(delay);
			Logger.info("Red");
			led.setValues(1, 0, 0);	// 100
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Blue");
			led.setValues(1, 0, 1);	// 101
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Green");
			led.setValues(1, 1, 0);	// 110
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Green + Blue");
			led.setValues(1, 1, 1);	// 111
			SleepUtil.sleepMillis(delay);
			
			float step = 0.01f;
			delay = 20;
			for (float r=0; r<=1; r+=step) {
				led.setValues(r, 0, 0);
				SleepUtil.sleepMillis(delay);
			}
			for (float r=1; r>=0; r-=step) {
				led.setValues(r, 0, 0);
				SleepUtil.sleepMillis(delay);
			}
			for (float g=0; g<=1; g+=step) {
				led.setValues(0, g, 0);
				SleepUtil.sleepMillis(delay);
			}
			for (float g=1; g>=0; g-=step) {
				led.setValues(0, g, 0);
				SleepUtil.sleepMillis(delay);
			}
			for (float b=0; b<=1; b+=step) {
				led.setValues(0, 0, b);
				SleepUtil.sleepMillis(delay);
			}
			for (float b=1; b>=0; b-=step) {
				led.setValues(0, 0, b);
				SleepUtil.sleepMillis(delay);
			}
		}
	}
}

