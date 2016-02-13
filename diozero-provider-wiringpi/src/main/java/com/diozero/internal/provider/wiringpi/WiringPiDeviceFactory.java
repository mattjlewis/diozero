package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Device I/O Zero - wiringPi provider
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
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDeviceFactory extends BaseNativeDeviceFactory {
	public WiringPiDeviceFactory() {
		// Initialise using native pin numbering scheme
		int status = Gpio.wiringPiSetupGpio();
		if (status != 0) {
			throw new RuntimeException("Error initialising wiringPi: " + status);
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new RuntimeIOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new WiringPiDigitalInputDevice(key, this, pinNumber, pud, trigger);
	}

	@Override
	public GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analogue devices aren't supported on this device");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new RuntimeIOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new WiringPiDigitalOutputDevice(key, this, pinNumber, initialValue);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new RuntimeIOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new WiringPiPwmOutputDevice(key, this, pinNumber, initialValue, pwmType);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException {
		return new WiringPiSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency)
			throws RuntimeIOException {
		return new WiringPiI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
