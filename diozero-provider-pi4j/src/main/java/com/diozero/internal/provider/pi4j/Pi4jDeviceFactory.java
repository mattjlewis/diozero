package com.diozero.internal.provider.pi4j;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - pi4j provider
 * Filename:     Pi4jDeviceFactory.java  
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
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.PwmPinInfo;
import com.diozero.api.PwmType;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.BaseNativeDeviceFactory;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.wiringpi.Gpio;

public class Pi4jDeviceFactory extends BaseNativeDeviceFactory {
	private static final int PI_PWM_CLOCK_BASE_FREQUENCY = 19_200_000;
	private static final int DEFAULT_HARDWARE_PWM_RANGE = 1024;
	private static final int DEFAULT_HARDWARE_PWM_FREQUENCY = 100;
	// See https://projects.drogon.net/raspberry-pi/wiringpi/software-pwm-library/
	// You can lower the range to get a higher frequency, at the expense of resolution,
	// or increase to get more resolution, but that will lower the frequency
	private static final int PI4J_MIN_SOFTWARE_PULSE_WIDTH_US = 100;
	
	private int boardPwmFrequency;
	private int hardwarePwmRange;
	
	private GpioController gpioController;
	
	public Pi4jDeviceFactory() {
		GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
		gpioController = GpioFactory.getInstance();
		
		// Default mode is balanced, actually want mark-space which gives traditional PWM with
		// predictable PWM frequencies
		setHardwarePwmFrequency(DEFAULT_HARDWARE_PWM_FREQUENCY);
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
		setHardwarePwmFrequency(pwmFrequency);
	}
	
	private void setHardwarePwmFrequency(int pwmFrequency) {
		// TODO Validate the requested PWM frequency
		hardwarePwmRange = DEFAULT_HARDWARE_PWM_RANGE;
		Gpio.pwmSetRange(hardwarePwmRange);
		int divisor = PI_PWM_CLOCK_BASE_FREQUENCY / hardwarePwmRange / pwmFrequency;
		Gpio.pwmSetClock(divisor);
		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
		this.boardPwmFrequency = pwmFrequency;
		Logger.info("setHardwarePwmFrequency({}) - range={}, divisor={}",
				Integer.valueOf(pwmFrequency), Integer.valueOf(hardwarePwmRange), Integer.valueOf(divisor));
	}
	
	private static int getSoftwarePwmRange(int pwmFrequency) {
		return 1_000_000 / (PI4J_MIN_SOFTWARE_PULSE_WIDTH_US * pwmFrequency);
	}
	
	@Override
	public int getSpiBufferSize() {
		return com.pi4j.io.spi.SpiDevice.MAX_SUPPORTED_BYTES;
	}
	
	@Override
	public void close() {
		super.close();
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
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		int gpio = pinInfo.getDeviceNumber();
		PwmType pwm_type = PwmType.SOFTWARE;
		if (pinInfo instanceof PwmPinInfo) {
			pwm_type = PwmType.HARDWARE;
			// PWM frequency is shared across all PWM outputs when using hardware PWM
			if (pwmFrequency != boardPwmFrequency) {
				Logger.warn("Requested PWM frequency ({}) is different to that configured for the board ({})"
						+ "; using board value", Integer.valueOf(pwmFrequency), Integer.valueOf(boardPwmFrequency));
			}
		}

		return new Pi4jPwmOutputDevice(key, this, gpioController, pwm_type, gpio, initialValue,
				pwm_type == PwmType.HARDWARE ? hardwarePwmRange : getSoftwarePwmRange(pwmFrequency));
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
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new Pi4jSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		return new Pi4jI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
