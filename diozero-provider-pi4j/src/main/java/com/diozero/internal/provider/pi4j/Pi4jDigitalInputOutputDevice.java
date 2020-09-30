package com.diozero.internal.provider.pi4j;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - pi4j provider
 * Filename:     Pi4jDigitalInputOutputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiBcmPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class Pi4jDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputOutputDeviceInterface, GpioPinListenerDigital {
	private GpioPinDigitalMultipurpose digitalInputOutputPin;
	private int gpio;
	private DeviceMode mode;
	
	Pi4jDigitalInputOutputDevice(String key, DeviceFactoryInterface deviceFactory, GpioController gpioController,
			int gpio, DeviceMode mode) {
		super(key, deviceFactory);
		
		Pin pin = RaspiBcmPin.getPinByAddress(gpio);
		if (pin == null) {
			throw new IllegalArgumentException("Illegal GPIO: " + gpio);
		}
		
		this.gpio = gpio;
		this.mode = mode;
		
		GpioPullUpDown pud = GpioPullUpDown.NONE;
		
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

		digitalInputOutputPin = gpioController.provisionDigitalMultipurposePin(pin,
				"Digital InputOutput for BCM GPIO " + gpio,
				mode == DeviceMode.DIGITAL_INPUT ? PinMode.DIGITAL_INPUT : PinMode.DIGITAL_OUTPUT, ppr);
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		digitalInputOutputPin.setMode(mode == DeviceMode.DIGITAL_INPUT ? PinMode.DIGITAL_INPUT : PinMode.DIGITAL_OUTPUT);
		this.mode = mode;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return digitalInputOutputPin.getState().isHigh();
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		digitalInputOutputPin.setState(value);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		digitalInputOutputPin.unexport();
		GpioFactory.getInstance().unprovisionPin(digitalInputOutputPin);
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		long nano_time = System.nanoTime();
		valueChanged(new DigitalInputEvent(gpio, System.currentTimeMillis(),
				nano_time, event.getState().isHigh()));
	}

	@Override
	public void enableListener() {
		digitalInputOutputPin.removeAllListeners();
		digitalInputOutputPin.addListener(this);
	}
	
	@Override
	public void disableListener() {
		digitalInputOutputPin.removeAllListeners();
	}
}
