package com.diozero.internal.spi;

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

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.util.RuntimeIOException;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used
 */
public abstract class BaseNativeDeviceFactory extends AbstractDeviceFactory implements NativeDeviceFactoryInterface {
	private static final String NATIVE_PREFIX = "Native-";
	private static final String GPIO_PREFIX = NATIVE_PREFIX + "GPIO-";
	private static final String I2C_PREFIX = NATIVE_PREFIX + "I2C-";
	private static final String SPI_PREFIX = NATIVE_PREFIX + "SPI-";
	
	private static String createGpioKey(int pinNumber) {
		return GPIO_PREFIX + pinNumber;
	}
	
	private static String createI2CKey(int controller, int address) {
		return I2C_PREFIX + controller + "-" + address;
	}
	
	private static String createSpiKey(int controller, int chipSelect) {
		return SPI_PREFIX + controller + "-" + chipSelect;
	}
	
	@Override
	public final GpioDigitalInputDeviceInterface provisionDigitalInputPin(int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		// TODO Understand limitations of a particular device
		// Create some sort of DeviceCapabilities class for this kind of information
		// Raspberry Pi GPIO2 and GPIO3 are SDA and SCL1 respectively
		if (pud == GpioPullUpDown.PULL_DOWN && (pinNumber == 2 || pinNumber == 3)) {
			throw new IllegalArgumentException("GPIO pins 2 and 3 are fitted with physical pull up resistors; "
					+ "you cannot initialize them in pull-down mode");
		}
		
		String key = createGpioKey(pinNumber);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalInputDeviceInterface device = createDigitalInputPin(key, pinNumber, pud, trigger);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final GpioAnalogInputDeviceInterface provisionAnalogInputPin(int pinNumber) throws RuntimeIOException {
		String key = createGpioKey(pinNumber);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogInputDeviceInterface device = createAnalogInputPin(key, pinNumber);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int pinNumber, boolean initialValue) throws RuntimeIOException {
		String key = createGpioKey(pinNumber);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalOutputDeviceInterface device = createDigitalOutputPin(key, pinNumber, initialValue);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final PwmOutputDeviceInterface provisionPwmOutputPin(int pinNumber, float initialValue) throws RuntimeIOException {
		String key = createGpioKey(pinNumber);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		// TODO Create some sort of DeviceCapabilities class for this kind of information
		// Raspberry Pi BCM GPIO pins with hardware PWM support: 12 (phys 32, wPi 26), 13 (phys 33, wPi 23),
		// 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
		PwmType pwm_type;
		if (pinNumber == 12 || pinNumber == 13 || pinNumber == 18 || pinNumber == 19) {
			pwm_type = PwmType.HARDWARE;
		} else {
			Logger.warn("Hardware PWM not available on BCM pin {}, reverting to software", Integer.valueOf(pinNumber));
			pwm_type = PwmType.SOFTWARE;
		}
		
		PwmOutputDeviceInterface device = createPwmOutputPin(key, pinNumber, initialValue, pwm_type);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final SpiDeviceInterface provisionSpiDevice(int controller, int chipSelect, int frequency, SpiClockMode spiClockMode) throws RuntimeIOException {
		String key = createSpiKey(controller, chipSelect);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		SpiDeviceInterface device = createSpiDevice(key, controller, chipSelect, frequency, spiClockMode);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final I2CDeviceInterface provisionI2CDevice(int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		String key = createI2CKey(controller, address);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		I2CDeviceInterface device = createI2CDevice(key, controller, address, addressSize, clockFrequency);
		deviceOpened(device);
		
		return device;
	}

	protected abstract GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException;
	protected abstract GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException;
	protected abstract GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws RuntimeIOException;
	protected abstract PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws RuntimeIOException;
	protected abstract SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException;
	protected abstract I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException;
}
