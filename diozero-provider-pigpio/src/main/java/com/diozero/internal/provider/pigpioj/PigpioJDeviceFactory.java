package com.diozero.internal.provider.pigpioj;

import org.pmw.tinylog.Logger;

/*
 * #%L
 * Device I/O Zero - pigpioj provider
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
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.pigpioj.PigpioGpio;
import com.diozero.util.RuntimeIOException;

public class PigpioJDeviceFactory extends BaseNativeDeviceFactory {
	public PigpioJDeviceFactory() {
		int rc = PigpioGpio.initialise();
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioGpio.initialise(), response: " + rc);
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int pinNumber) {
		return PigpioGpio.getPWMFrequency(pinNumber);
	}
	
	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		int old_freq = PigpioGpio.getPWMFrequency(pinNumber);
		int old_range = PigpioGpio.getPWMRange(pinNumber);
		int old_real_range = PigpioGpio.getPWMRealRange(pinNumber);
		PigpioGpio.setPWMFrequency(pinNumber, pwmFrequency);
		PigpioGpio.setPWMRange(pinNumber, PigpioGpio.getPWMRealRange(pinNumber));
		int new_freq = PigpioGpio.getPWMFrequency(pinNumber);
		int new_range = PigpioGpio.getPWMRange(pinNumber);
		int new_real_range = PigpioGpio.getPWMRealRange(pinNumber);
		Logger.info("setPwmFrequency({}, {}), old freq={}, old real range={}, old range={},"
				+ " new freq={}, new real range={}, new range={}",
				Integer.valueOf(pinNumber), Integer.valueOf(pwmFrequency),
				Integer.valueOf(old_freq), Integer.valueOf(old_real_range), Integer.valueOf(old_range),
				Integer.valueOf(new_freq), Integer.valueOf(new_real_range), Integer.valueOf(new_range));
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		PigpioGpio.terminate();
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog input pins not supported");
	}

	@Override
	protected GpioAnalogOutputDeviceInterface createAnalogOutputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new PigpioJDigitalInputDevice(key, this, pinNumber, pud, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws RuntimeIOException {
		return new PigpioJDigitalOutputDevice(key, this, pinNumber, initialValue);
	}
	
	protected GpioDeviceInterface.Mode getCurrentGpioMode(int pinNumber) {
		int rc = PigpioGpio.getMode(pinNumber);
		
		if (rc == PigpioGpio.MODE_PI_INPUT) {
			return Mode.DIGITAL_INPUT;
		} else if (rc == PigpioGpio.MODE_PI_OUTPUT) {
			return Mode.DIGITAL_OUTPUT;
		}
		
		throw new RuntimeIOException("Error calling PigpioGpio.getMode(), response: " + rc);
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber, float initialValue,
			PwmType pwmType) throws RuntimeIOException {
		return new PigpioJPwmOutputDevice(key, this, pinNumber, initialValue, PigpioGpio.getPWMRange(pinNumber));
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException {
		return new PigpioJSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new PigpioJI2CDevice(key, this, controller, address, addressSize);
	}
}
