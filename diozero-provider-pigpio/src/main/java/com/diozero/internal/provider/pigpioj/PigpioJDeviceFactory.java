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
import com.diozero.pigpioj.PigpioGpio;
import com.diozero.util.RuntimeIOException;

public class PigpioJDeviceFactory extends BaseNativeDeviceFactory {
	private int boardPwmFrequency;

	public PigpioJDeviceFactory() {
		initialiseBoardInfo();
		
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
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}
	
	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		boardPwmFrequency = pwmFrequency;
	}
	
	@Override
	public void close() {
		super.close();
		PigpioGpio.terminate();
	}

	protected DeviceMode getCurrentGpioMode(int gpio) {
		int rc = PigpioGpio.getMode(gpio);
		
		if (rc == PigpioGpio.MODE_PI_INPUT) {
			return DeviceMode.DIGITAL_INPUT;
		} else if (rc == PigpioGpio.MODE_PI_OUTPUT) {
			return DeviceMode.DIGITAL_OUTPUT;
		}
		
		throw new RuntimeIOException("Error calling PigpioGpio.getMode(), response: " + rc);
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new PigpioJDigitalInputDevice(key, this, pinInfo.getDeviceNumber(), pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new PigpioJDigitalOutputDevice(key, this, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		return new PigpioJDigitalInputOutputDevice(key, this, pinInfo.getDeviceNumber(), mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		return new PigpioJPwmOutputDevice(key, this, pinInfo.getDeviceNumber(), initialValue,
				setPwmFrequency(pinInfo.getDeviceNumber(), pwmFrequency));
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog input pins not supported");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new PigpioJSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new PigpioJI2CDevice(key, this, controller, address, addressSize);
	}
	
	public PigpioJBitBangI2CDevice createI2CBitBangDevice(int sdaPin, int sclPin, int baud) {
		return new PigpioJBitBangI2CDevice("PigpioJ-BitBangI2C-" + sdaPin, this, sdaPin, sclPin, baud);
	}
	
	static int getPigpioJPullUpDown(GpioPullUpDown pud) {
		int pigpio_pud;
		switch (pud) {
		case PULL_DOWN:
			pigpio_pud = PigpioGpio.PI_PUD_DOWN;
			break;
		case PULL_UP:
			pigpio_pud = PigpioGpio.PI_PUD_UP;
			break;
		case NONE:
		default:
			pigpio_pud = PigpioGpio.PI_PUD_OFF;
			break;
		}
		return pigpio_pud;
	}

	private static int setPwmFrequency(int gpio, int pwmFrequency) {
		int old_freq = PigpioGpio.getPWMFrequency(gpio);
		int old_range = PigpioGpio.getPWMRange(gpio);
		int old_real_range = PigpioGpio.getPWMRealRange(gpio);
		PigpioGpio.setPWMFrequency(gpio, pwmFrequency);
		PigpioGpio.setPWMRange(gpio, PigpioGpio.getPWMRealRange(gpio));
		int new_freq = PigpioGpio.getPWMFrequency(gpio);
		int new_range = PigpioGpio.getPWMRange(gpio);
		int new_real_range = PigpioGpio.getPWMRealRange(gpio);
		Logger.info("setPwmFrequency({}, {}), old freq={}, old real range={}, old range={},"
				+ " new freq={}, new real range={}, new range={}",
				Integer.valueOf(gpio), Integer.valueOf(pwmFrequency),
				Integer.valueOf(old_freq), Integer.valueOf(old_real_range), Integer.valueOf(old_range),
				Integer.valueOf(new_freq), Integer.valueOf(new_real_range), Integer.valueOf(new_range));
		
		return new_range;
	}
}
