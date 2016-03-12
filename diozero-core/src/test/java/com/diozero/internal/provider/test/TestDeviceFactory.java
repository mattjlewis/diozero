package com.diozero.internal.provider.test;

import java.lang.reflect.Constructor;

/*
 * #%L
 * Device I/O Zero - Core
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

import com.diozero.api.*;
import com.diozero.internal.DeviceStates;
import com.diozero.internal.spi.*;
import com.diozero.util.RuntimeIOException;
public class TestDeviceFactory extends BaseNativeDeviceFactory {
	private static Class<? extends GpioDigitalInputDeviceInterface> digitalInputDeviceClass;
	private static Class<? extends GpioDigitalOutputDeviceInterface> digitalOutputDeviceClass;

	public static void setDigitalInputDeviceClass(Class<? extends GpioDigitalInputDeviceInterface> clz) {
		digitalInputDeviceClass = clz;
	}
	
	public static void setDigitalOutputDeviceClass(Class<? extends GpioDigitalOutputDeviceInterface> clz) {
		digitalOutputDeviceClass = clz;
	}
	
	// Added for testing purposes only
	public DeviceStates getDeviceStates() {
		return deviceStates;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (digitalInputDeviceClass == null) {
			return new TestDigitalInputDevice(key, this, pinNumber, pud, trigger);
		}
		
		try {
			return digitalInputDeviceClass.getConstructor(String.class, DeviceFactoryInterface.class, Integer.TYPE, GpioPullUpDown.class, GpioEventTrigger.class)
				.newInstance(key, this, Integer.valueOf(pinNumber), pud, trigger);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		// TODO Support for test analog GPIO pins
		throw new UnsupportedOperationException("Analog GPIOs not yet supported in the Test device provider factory");
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws RuntimeIOException {
		if (digitalOutputDeviceClass == null) {
			return new TestDigitalOutputDevice(key, this, pinNumber, initialValue);
		}
		
		try {
			return digitalOutputDeviceClass.getConstructor(String.class, DeviceFactoryInterface.class, Integer.TYPE, Boolean.TYPE)
				.newInstance(key, this, Integer.valueOf(pinNumber), Boolean.valueOf(initialValue));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		// TODO Support for test PWM devices
		throw new UnsupportedOperationException("PWM not yet supported in the Test device provider factory");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException {
		// TODO Ability to create different SPI devices
		return new TestMcpAdcSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		// TODO Support for test I2C devices
		return new TestI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
