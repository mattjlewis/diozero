package com.diozero.internal.spi;

import java.util.ArrayList;
import java.util.List;

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
import com.diozero.util.BoardInfo;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SystemInfo;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used
 */

public abstract class BaseNativeDeviceFactory extends AbstractDeviceFactory implements NativeDeviceFactoryInterface {
	private static final String NATIVE_PREFIX = "Native-";
	private static final String GPIO_PREFIX = NATIVE_PREFIX + "GPIO-";
	private static final String PWM_PREFIX = NATIVE_PREFIX + "PWM-";
	private static final String I2C_PREFIX = NATIVE_PREFIX + "I2C-";
	private static final String SPI_PREFIX = NATIVE_PREFIX + "SPI-";
	
	private static String createPwmKey(int pwmNum) {
		return PWM_PREFIX + pwmNum;
	}
	
	private static String createI2CKey(int controller, int address) {
		return I2C_PREFIX + controller + "-" + address;
	}
	
	private static String createSpiKey(int controller, int chipSelect) {
		return SPI_PREFIX + controller + "-" + chipSelect;
	}
	
	private List<DeviceFactoryInterface> deviceFactories = new ArrayList<>();
	protected BoardInfo boardInfo;
	
	public BaseNativeDeviceFactory() {
		super(GPIO_PREFIX);
		
		boardInfo = SystemInfo.getBoardInfo();
	}
	
	@Override
	public final void registerDeviceFactory(DeviceFactoryInterface deviceFactory) {
		deviceFactories.add(deviceFactory);
	}
	
	@Override
	public void shutdown() {
		for (DeviceFactoryInterface df : deviceFactories) {
			if (! df.isShutdown()) {
				df.shutdown();
			}
		}
		super.shutdown();
	}

	@Override
	public final GpioAnalogInputDeviceInterface provisionAnalogInputPin(int gpio) throws RuntimeIOException {
		int mapped_gpio = boardInfo.mapGpio(gpio);
		if (! boardInfo.isSupported(DeviceMode.ANALOG_INPUT, mapped_gpio)) {
			throw new IllegalArgumentException("Invalid mode (analog input) for GPIO " + mapped_gpio);
		}
		
		String key = createPinKey(mapped_gpio);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogInputDeviceInterface device = createAnalogInputPin(key, mapped_gpio);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final GpioAnalogOutputDeviceInterface provisionAnalogOutputPin(int gpio) throws RuntimeIOException {
		int mapped_gpio = boardInfo.mapGpio(gpio);
		if (! boardInfo.isSupported(DeviceMode.ANALOG_OUTPUT, mapped_gpio)) {
			throw new IllegalArgumentException("Invalid mode (analog output) for GPIO " + mapped_gpio);
		}
		
		String key = createPinKey(mapped_gpio);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogOutputDeviceInterface device = createAnalogOutputPin(key, mapped_gpio);
		deviceOpened(device);
		
		return device;
	}
	
	@Override
	public final GpioDigitalInputDeviceInterface provisionDigitalInputPin(int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		int mapped_gpio = boardInfo.mapGpio(gpio);
		if (! boardInfo.isSupported(DeviceMode.DIGITAL_INPUT, mapped_gpio)) {
			throw new IllegalArgumentException("Invalid mode (digital input) for gpio " + mapped_gpio);
		}
		
		String key = createPinKey(mapped_gpio);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalInputDeviceInterface device = createDigitalInputPin(key, mapped_gpio, pud, trigger);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int gpio, boolean initialValue) throws RuntimeIOException {
		int mapped_gpio = boardInfo.mapGpio(gpio);
		if (! boardInfo.isSupported(DeviceMode.DIGITAL_OUTPUT, mapped_gpio)) {
			throw new IllegalArgumentException("Invalid mode (digital output) for gpio " + mapped_gpio);
		}
		
		String key = createPinKey(mapped_gpio);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalOutputDeviceInterface device = createDigitalOutputPin(key, mapped_gpio, initialValue);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final GpioDigitalInputOutputDeviceInterface provisionDigitalInputOutputPin(int gpio, DeviceMode mode) throws RuntimeIOException {
		int mapped_gpio = boardInfo.mapGpio(gpio);
		if (! boardInfo.isSupported(DeviceMode.DIGITAL_OUTPUT, mapped_gpio) ||
				! boardInfo.isSupported(DeviceMode.DIGITAL_INPUT, mapped_gpio)) {
			throw new IllegalArgumentException("Invalid mode (digital input/output) for gpio " + mapped_gpio);
		}
		
		String key = createPinKey(mapped_gpio);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalInputOutputDeviceInterface device = createDigitalInputOutputPin(key, mapped_gpio, mode);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final PwmOutputDeviceInterface provisionPwmOutputPin(int gpio, float initialValue) throws RuntimeIOException {
		int mapped_gpio;
		String key;
		PwmType pwm_type;
		if (boardInfo.isSupported(DeviceMode.PWM_OUTPUT, gpio)) {
			pwm_type = PwmType.HARDWARE;
			mapped_gpio = gpio;
			key = createPwmKey(mapped_gpio);
		} else if (boardInfo.isSupported(DeviceMode.DIGITAL_OUTPUT, gpio)) {
			Logger.warn("Hardware PWM not available on pin {}, reverting to software", Integer.valueOf(gpio));
			pwm_type = PwmType.SOFTWARE;
			mapped_gpio = boardInfo.mapGpio(gpio);
			key = createPinKey(mapped_gpio);
		} else {
			throw new IllegalArgumentException("Invalid mode (PWM output) for GPIO " + gpio);
		}
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}

		PwmOutputDeviceInterface device = createPwmOutputPin(key, mapped_gpio, initialValue, pwm_type);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final SpiDeviceInterface provisionSpiDevice(int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		String key = createSpiKey(controller, chipSelect);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		SpiDeviceInterface device = createSpiDevice(key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
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

	protected abstract GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int gpio) throws RuntimeIOException;
	protected abstract GpioAnalogOutputDeviceInterface createAnalogOutputPin(String key, int gpio) throws RuntimeIOException;
	protected abstract GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException;
	protected abstract GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int gpio, boolean initialValue) throws RuntimeIOException;
	protected abstract GpioDigitalInputOutputDeviceInterface createDigitalInputOutputPin(String key, int gpio, DeviceMode mode) throws RuntimeIOException;
	protected abstract PwmOutputDeviceInterface createPwmOutputPin(String key, int gpio,
			float initialValue, PwmType pwmType) throws RuntimeIOException;
	protected abstract SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException;
	protected abstract I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException;
}
