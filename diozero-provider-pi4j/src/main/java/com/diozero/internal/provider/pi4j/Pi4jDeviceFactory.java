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
import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.Gpio;

public class Pi4jDeviceFactory extends BaseNativeDeviceFactory {
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
	
	private GpioController gpioController;
	
	public Pi4jDeviceFactory() {
		initialiseBoardInfo();
		
		GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
		gpioController = GpioFactory.getInstance();
		
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
		if (boardInfo.getByGpioNumber(gpio).isSupported(DeviceMode.PWM_OUTPUT)) {
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
		if (boardInfo.getByGpioNumber(gpio).isSupported(DeviceMode.PWM_OUTPUT)) {
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
	public void shutdown() {
		super.shutdown();
		gpioController.shutdown();
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo,
			GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		return new Pi4jDigitalInputDevice(key, this, gpioController, pinInfo.getDeviceNumber(), pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) throws RuntimeIOException {
		return new Pi4jDigitalOutputDevice(key, this, gpioController, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode)
			throws RuntimeIOException {
		return new Pi4jDigitalInputOutputDevice(key, this, gpioController, pinInfo.getDeviceNumber(), mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, float initialValue)
			throws RuntimeIOException {
		int gpio = pinInfo.getDeviceNumber();
		PwmType pwm_type = PwmType.SOFTWARE;
		if (pinInfo instanceof PwmPinInfo) {
			pwm_type = PwmType.HARDWARE;
		}

		return new Pi4jPwmOutputDevice(key, this, gpioController, pwm_type, gpio, initialValue,
				pwm_type == PwmType.HARDWARE ? hardwarePwmRange : getSoftwarePwmRange(gpio));
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new Pi4jSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		return new Pi4jI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
