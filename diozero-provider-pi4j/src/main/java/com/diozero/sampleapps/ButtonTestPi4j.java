package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - pi4j provider
 * %%
 * Copyright (C) 2016 diozero
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
