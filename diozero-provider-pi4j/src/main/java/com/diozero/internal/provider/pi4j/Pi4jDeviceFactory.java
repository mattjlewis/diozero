package com.diozero.internal.provider.pi4j;

import java.util.HashMap;
import java.util.Map;

import org.pmw.tinylog.Logger;

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
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class Pi4jDeviceFactory extends BaseNativeDeviceFactory {
	private static final int PI_PWM_CLOCK_BASE_FREQUENCY = 19_200_000;
	private static final int DEFAULT_HARDWARE_PWM_RANGE = 1024;
	private static final int DEFAULT_SOFTWARE_PWM_FREQ = 100;
	// See https://projects.drogon.net/raspberry-pi/wiringpi/software-pwm-library/
	// You can lower the range to get a higher frequency, at the expense of resolution,
	// or increase to get more resolution, but that will lower the frequency
	private static final int PI4J_MIN_SOFTWARE_PULSE_WIDTH_US = 100;
	
	private Map<Integer, Integer> softwarePwmFrequency;
	private int hardwarePwmFrequency;
	private int hardwarePwmRange;
	
	private GpioController gpioController;
	
	public Pi4jDeviceFactory() {
		gpioController = GpioFactory.getInstance();
		
		// Default mode is balanced, actually want mark-space which gives traditional PWM with
		// predictable PWM frequencies
		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
		
		softwarePwmFrequency = new HashMap<>();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int pinNumber) {
		if (pinNumber == 12 || pinNumber == 13 || pinNumber == 18 || pinNumber == 19) {
			return hardwarePwmFrequency;
		}
		
		Integer pwm_freq = softwarePwmFrequency.get(Integer.valueOf(pinNumber));
		if (pwm_freq == null) {
			return DEFAULT_SOFTWARE_PWM_FREQ;
		}
		
		return pwm_freq.intValue();
	}
	
	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		if (pinNumber == 12 || pinNumber == 13 || pinNumber == 18 || pinNumber == 19) {
			// TODO Validate the requested PWM frequency
			hardwarePwmRange = DEFAULT_HARDWARE_PWM_RANGE;
			Gpio.pwmSetRange(hardwarePwmRange);
			int divisor = PI_PWM_CLOCK_BASE_FREQUENCY / hardwarePwmRange / pwmFrequency;
			Gpio.pwmSetClock(divisor);
			this.hardwarePwmFrequency = pwmFrequency;
			Logger.info("setHardwarePwmFrequency({}, {}) - range={}, divisor={}", Integer.valueOf(pinNumber),
					Integer.valueOf(pwmFrequency), Integer.valueOf(hardwarePwmRange), Integer.valueOf(divisor));
		} else {
			// TODO Software PWM frequency should be limited to 20..250Hz (gives a range of 500..40)
			this.softwarePwmFrequency.put(Integer.valueOf(pinNumber), Integer.valueOf(pwmFrequency));
			Logger.info("setPwmFrequency({}, {}) - range={}", Integer.valueOf(pinNumber),
					Integer.valueOf(pwmFrequency), Integer.valueOf(getSoftwarePwmRange(pinNumber)));
		}
	}
	
	private int getSoftwarePwmRange(int pinNumber) {
		return 1_000_000 / (PI4J_MIN_SOFTWARE_PULSE_WIDTH_US * getPwmFrequency(pinNumber));
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new Pi4jGpioInputDevice(key, this, gpioController, pinNumber, pud, trigger);
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws RuntimeIOException {
		return new Pi4jGpioOutputDevice(key, this, gpioController, pinNumber, initialValue);
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new RuntimeIOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new Pi4jPwmOutputDevice(key, this, gpioController, pwmType,
				pwmType == PwmType.HARDWARE ? hardwarePwmRange : getSoftwarePwmRange(pinNumber), pinNumber, initialValue);
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency, SpiClockMode spiClockMode) throws RuntimeIOException {
		return new Pi4jSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		return new Pi4jI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
