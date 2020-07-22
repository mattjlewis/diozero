package com.diozero.internal.provider.pi4j;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - pi4j provider
 * Filename:     Pi4jDigitalInputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.GpioUtil;

public class Pi4jDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, GpioPinListenerDigital {
	private GpioPinDigitalInput digitalInputPin;
	private int gpio;
	
	Pi4jDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, GpioController gpioController,
			int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		Pin pin = RaspiBcmPin.getPinByAddress(gpio);
		if (pin == null) {
			throw new IllegalArgumentException("Illegal GPIO number: " + gpio);
		}
		
		this.gpio = gpio;
		
		PinPullResistance ppr;
		switch (pud) {
		case PULL_DOWN:
			ppr = PinPullResistance.PULL_DOWN;
			break;
		case PULL_UP:
			ppr = PinPullResistance.PULL_UP;
			break;
		case NONE:
		default:
			ppr = PinPullResistance.OFF;
		}

		/*
		PinEdge edge;
		switch (trigger) {
		case FALLING:
			edge = PinEdge.FALLING;
			break;
		case RISING:
			edge = PinEdge.RISING;
			break;
		case NONE:
			edge = PinEdge.NONE;
			break;
		case BOTH:
		default:
			edge = PinEdge.BOTH;
			break;
		}
		*/
		
		// Note configuring GPIO event trigger values (rising / falling / both) via the provision APIs isn't possible in Pi4j
		digitalInputPin = gpioController.provisionDigitalInputPin(
				pin, "Digital Input for BCM GPIO " + gpio, ppr);
		
		// RaspiGpioProvider.export() calls this for all input pins:
		if (! GpioUtil.setEdgeDetection(pin.getAddress(), PinEdge.BOTH.getValue())) {
			throw new RuntimeIOException("Error setting edge detection");
		}
		//GpioUtil.setEdgeDetection(pin.getAddress(), edge.getValue());
	}

	@Override
	protected void closeDevice() {
		Logger.debug("closeDevice()");
		removeListener();
		digitalInputPin.removeAllTriggers();
		digitalInputPin.unexport();
		GpioFactory.getInstance().unprovisionPin(digitalInputPin);
	}

	@Override
	public boolean getValue() {
		return digitalInputPin.getState().isHigh();
	}

	@Override
	public int getGpio() {
		return gpio;
	}
	
	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		digitalInputPin.setDebounce(debounceTime);
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		long nano_time = System.nanoTime();
		valueChanged(new DigitalInputEvent(gpio, System.currentTimeMillis(),
				nano_time, event.getState().isHigh()));
	}

	@Override
	public void enableListener() {
		digitalInputPin.removeAllListeners();
		digitalInputPin.addListener(this);
	}
	
	@Override
	public void disableListener() {
		digitalInputPin.removeAllListeners();
	}
}
