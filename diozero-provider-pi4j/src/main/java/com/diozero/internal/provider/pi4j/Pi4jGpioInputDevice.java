package com.diozero.internal.provider.pi4j;

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

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.GpioUtil;

public class Pi4jGpioInputDevice extends AbstractInputDevice<DigitalPinEvent> implements GpioDigitalInputDeviceInterface, GpioPinListenerDigital {
	private GpioPinDigitalInput digitalInputPin;
	private int pinNumber;
	
	Pi4jGpioInputDevice(String key, DeviceFactoryInterface deviceFactory, GpioController gpioController,
			int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		Pin pin = RaspiGpioBcm.getPin(pinNumber);
		if (pin == null) {
			throw new IllegalArgumentException("Illegal pin number: " + pinNumber);
		}
		
		this.pinNumber = pinNumber;
		
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
		
		// Note configuring GPIO event trigger values (rising / falling / both) via the provision APIs isn't possible in Pi4j
		digitalInputPin = gpioController.provisionDigitalInputPin(pin, "Digital Input for BCM GPIO " + pinNumber, ppr);
		
		// RaspiGpioProvider.export() calls this for all input pins:
		if (!GpioUtil.setEdgeDetection(pin.getAddress(), PinEdge.BOTH.getValue())) {
			throw new RuntimeIOException("Error setting edge detection");
		}
		//GpioUtil.setEdgeDetection(pin.getAddress(), edge.getValue());
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		removeListener();
		digitalInputPin.removeAllTriggers();
		digitalInputPin.unexport();
	}

	@Override
	public boolean getValue() {
		return digitalInputPin.getState().isHigh();
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
	
	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		digitalInputPin.setDebounce(debounceTime);
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		long nano_time = System.nanoTime();
		valueChanged(new DigitalPinEvent(pinNumber, System.currentTimeMillis(),
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
