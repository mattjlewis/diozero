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

import com.diozero.api.*;
import com.diozero.internal.spi.*;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;

public class Pi4jDeviceFactory extends BaseNativeDeviceFactory {
	private GpioController gpioController;
	
	public Pi4jDeviceFactory() {
		gpioController = GpioFactory.getInstance();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new Pi4jGpioInputDevice(key, this, gpioController, pinNumber, pud, trigger);
	}

	@Override
	public GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analogue devices aren't supported on this device");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws RuntimeIOException {
		return new Pi4jGpioOutputDevice(key, this, gpioController, pinNumber, initialValue);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		return new Pi4jPwmOutputDevice(key, this, gpioController, pinNumber, initialValue, pwmType);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency, SpiClockMode spiClockMode) throws RuntimeIOException {
		return new Pi4jSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		return new Pi4jI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
