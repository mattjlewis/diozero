package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - wiringPi provider
 * Filename:     ButtonTestWiringPi.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


import com.diozero.util.SleepUtil;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterruptCallback;
import com.pi4j.wiringpi.GpioUtil;

public class ButtonTestWiringPi implements GpioInterruptCallback {
	public static void main(String[] args) {
		new ButtonTestWiringPi().test(12);
	}
	
	public void test(int gpio) {
		int status = Gpio.wiringPiSetupGpio();
		if (status != 0) {
			throw new RuntimeException("Error initialising wiringPi: " + status);
		}
		Gpio.pinMode(gpio, Gpio.INPUT);
		Gpio.pullUpDnControl(gpio, Gpio.PUD_UP);
		int delay = 20;
		System.out.println("Waiting " + delay + "s for events..., thread name=" + Thread.currentThread().getName());
		if (Gpio.wiringPiISR(gpio, Gpio.INT_EDGE_BOTH, this) != 1) {
			System.out.println("Error in wiringPiISR");
		} else {
			System.out.println("Sleeping for " + delay + "s");
			SleepUtil.sleepSeconds(delay);
		}
		
		GpioUtil.unexport(gpio);
	}

	@Override
	public void callback(int pin) {
		System.out.println("callback(" + pin + "), thread name=" + Thread.currentThread().getName());
	}
}
