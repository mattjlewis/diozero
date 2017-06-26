package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Sample applications
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.devices.PCF8591;
import com.diozero.devices.PwmLed;
import com.diozero.util.DeviceFactoryHelper;
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
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
}
