package com.diozero.sampleapps;

import com.diozero.util.SleepUtil;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterruptCallback;
import com.pi4j.wiringpi.GpioUtil;

public class ButtonTestWiringPi implements GpioInterruptCallback {
	public static void main(String[] args) {
		new ButtonTestWiringPi().test(12);
	}
	
	public void test(int pinNumber) {
		int status = Gpio.wiringPiSetupGpio();
		if (status != 0) {
			throw new RuntimeException("Error initialising wiringPi: " + status);
		}
		Gpio.pinMode(pinNumber, Gpio.INPUT);
		Gpio.pullUpDnControl(pinNumber, Gpio.PUD_UP);
		int delay = 20;
		System.out.println("Waiting " + delay + "s for events..., thread name=" + Thread.currentThread().getName());
		if (Gpio.wiringPiISR(pinNumber, Gpio.INT_EDGE_BOTH, this) != 1) {
			System.out.println("Error in wiringPiISR");
		} else {
			System.out.println("Sleeping for " + delay + "s");
			SleepUtil.sleepSeconds(delay);
		}
		
		GpioUtil.unexport(pinNumber);
	}

	@Override
	public void callback(int pin) {
		System.out.println("callback(" + pin + "), thread name=" + Thread.currentThread().getName());
	}
}
