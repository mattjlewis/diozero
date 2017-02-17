package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.PCF8591;
import com.diozero.PwmLed;
import com.diozero.api.AnalogInputDevice;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.SleepUtil;

public class JoystickTest {
	public static void main(String[] args) {
		if (args.length < 4) {
			Logger.error("Usage: {} <adc1> <adc2> <pwm1> <pwm2>", JoystickTest.class);
			System.exit(1);
		}
		
		int adc_num1 = Integer.parseInt(args[0]);
		int adc_num2 = Integer.parseInt(args[1]);
		int pwm1 = Integer.parseInt(args[2]);
		int pwm2 = Integer.parseInt(args[3]);
		
		test(adc_num1, adc_num2, pwm1, pwm2);
	}
	
	private static void test(int adcNum1, int adcNum2, int pwm1, int pwm2) {
		try (PCF8591 adc = new PCF8591(1);
				AnalogInputDevice axis_1 = new AnalogInputDevice(adc, adcNum1);
				AnalogInputDevice axis_2 = new AnalogInputDevice(adc, adcNum2);
				PwmLed led1 = new PwmLed(pwm1);
				PwmLed led2 = new PwmLed(pwm2)) {
			axis_1.addListener(event -> led1.setValue(event.getUnscaledValue()));
			axis_2.addListener(event -> led2.setValue(event.getUnscaledValue()));
			for (int i=0; i<20; i++) {
				Logger.info("axis 1: {}, axis 2: {}", Float.valueOf(axis_1.getScaledValue()), Float.valueOf(axis_2.getScaledValue()));
				
				SleepUtil.sleepSeconds(1);
			}
		}
		DioZeroScheduler.shutdownAll();
	}
}
