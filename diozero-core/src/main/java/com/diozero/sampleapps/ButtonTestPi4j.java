package com.diozero.sampleapps;

import com.diozero.internal.provider.pi4j.RaspiGpioBcm;
import com.diozero.util.SleepUtil;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.GpioUtil;

public class ButtonTestPi4j implements GpioPinListenerDigital {
	public static void main(String[] args) {
		new ButtonTestPi4j().test(12);
	}
	
	public void test(int pinNumber) {
		GpioController gpio_controller = GpioFactory.getInstance();
		Pin pin = RaspiGpioBcm.getPin(pinNumber);
		GpioPinDigitalInput digitalInputPin = gpio_controller.provisionDigitalInputPin(pin,
				"Digital Input for BCM GPIO " + pinNumber, PinPullResistance.PULL_UP);
		GpioUtil.setEdgeDetection(pin.getAddress(), PinEdge.BOTH.getValue());
		digitalInputPin.addListener(this);
		System.out.println("Waiting 20s for events..., thread name=" + Thread.currentThread().getName());
		SleepUtil.sleepSeconds(20);
		gpio_controller.unprovisionPin(digitalInputPin);
		gpio_controller.shutdown();
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		System.out.println("handleGpioPinDigitalStateChangeEvent(" + event.getState().getValue() + ")");
	}
}
