package com.diozero.internal.provider.wiringpi;

import java.util.HashMap;
import java.util.Map;

import org.pmw.tinylog.Logger;

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
	private static final int PI_PWM_CLOCK_BASE_FREQUENCY = 19_200_000;
	private static final int DEFAULT_HARDWARE_PWM_RANGE = 1024;
	private static final int DEFAULT_HARDWARE_PWM_FREQUENCY = 100;
	private static final int DEFAULT_SOFTWARE_PWM_FREQ = 100;
	// See https://projects.drogon.net/raspberry-pi/wiringpi/software-pwm-library/
	// You can lower the range to get a higher frequency, at the expense of resolution,
	// or increase to get more resolution, but that will lower the frequency
	private static final int PI4J_MIN_SOFTWARE_PULSE_WIDTH_US = 100;
	
	private Map<Integer, Integer> softwarePwmFrequency;
	private int hardwarePwmFrequency;
	private int hardwarePwmRange;
	
	public WiringPiDeviceFactory() {
		// Initialise using native pin numbering scheme
		int status = Gpio.wiringPiSetupGpio();
		if (status != 0) {
			throw new RuntimeException("Error initialising wiringPi: " + status);
		}
		
		// Default mode is balanced, actually want mark-space which gives traditional PWM with
		// predictable PWM frequencies
		setHardwarePwmFrequency(DEFAULT_HARDWARE_PWM_FREQUENCY);
		
		softwarePwmFrequency = new HashMap<>();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int gpio) {
		if (boardInfo.isSupported(DeviceMode.PWM_OUTPUT, gpio)) {
			return hardwarePwmFrequency;
		}
		
		Integer pwm_freq = softwarePwmFrequency.get(Integer.valueOf(gpio));
		if (pwm_freq == null) {
			return DEFAULT_SOFTWARE_PWM_FREQ;
		}
		
		return pwm_freq.intValue();
	}
	
	@Override
	public void setPwmFrequency(int gpio, int pwmFrequency) {
		if (boardInfo.isSupported(DeviceMode.PWM_OUTPUT, gpio)) {
			setHardwarePwmFrequency(pwmFrequency);
		} else {
			// TODO Software PWM frequency should be limited to 20..250Hz (gives a range of 500..40)
			this.softwarePwmFrequency.put(Integer.valueOf(gpio), Integer.valueOf(pwmFrequency));
			Logger.info("setPwmFrequency({}, {}) - range={}", Integer.valueOf(gpio),
					Integer.valueOf(pwmFrequency), Integer.valueOf(getSoftwarePwmRange(gpio)));
		}
	}
	
	private void setHardwarePwmFrequency(int pwmFrequency) {
		// TODO Validate the requested PWM frequency
		hardwarePwmRange = DEFAULT_HARDWARE_PWM_RANGE;
		Gpio.pwmSetRange(hardwarePwmRange);
		int divisor = PI_PWM_CLOCK_BASE_FREQUENCY / hardwarePwmRange / pwmFrequency;
		Gpio.pwmSetClock(divisor);
		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
		this.hardwarePwmFrequency = pwmFrequency;
		Logger.info("setHardwarePwmFrequency({}) - range={}, divisor={}",
				Integer.valueOf(pwmFrequency), Integer.valueOf(hardwarePwmRange), Integer.valueOf(divisor));
	}

	private int getSoftwarePwmRange(int gpio) {
		return 1_000_000 / (PI4J_MIN_SOFTWARE_PULSE_WIDTH_US * getPwmFrequency(gpio));
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int gpio) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected GpioAnalogOutputDeviceInterface createAnalogOutputPin(String key, int gpio) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(gpio) != 1) {
			throw new RuntimeIOException("Error: Pin " + gpio + " isn't supported");
		}
		
		return new WiringPiDigitalInputDevice(key, this, gpio, pud, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int gpio, boolean initialValue) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(gpio) != 1) {
			throw new RuntimeIOException("Error: Pin " + gpio + " isn't supported");
		}
		
		return new WiringPiDigitalOutputDevice(key, this, gpio, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputPin(String key, int gpio, DeviceMode mode)
			throws RuntimeIOException {
		throw new UnsupportedOperationException("Digital Input / Output devices not yet supported by this provider");
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int gpio,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		if (GpioUtil.isPinSupported(gpio) != 1) {
			throw new RuntimeIOException("Error: Pin " + gpio + " isn't supported");
		}
		
		return new WiringPiPwmOutputDevice(key, this, pwmType,
				pwmType == PwmType.HARDWARE ? hardwarePwmRange : getSoftwarePwmRange(gpio), gpio, initialValue);
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new WiringPiSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency)
			throws RuntimeIOException {
		return new WiringPiI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
