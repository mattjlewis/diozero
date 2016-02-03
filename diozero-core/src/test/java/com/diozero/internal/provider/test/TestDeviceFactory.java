package com.diozero.internal.provider.test;

import java.io.IOException;

import com.diozero.api.*;
import com.diozero.internal.spi.*;

public class TestDeviceFactory extends BaseNativeDeviceFactory {

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws IOException {
		return new TestDigitalInputPin(key, this, pinNumber, pud, trigger);
	}

	@Override
	protected GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws IOException {
		// TODO Support for test analogue GPIO pins
		throw new UnsupportedOperationException("Analogue GPIOs not yet supported in the Test device provider factory");
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws IOException {
		return new TestDigitalOutputPin(key, this, pinNumber, initialValue);
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws IOException {
		// TODO Support for test PWM devices
		throw new UnsupportedOperationException("PWM not yet supported in the Test device provider factory");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws IOException {
		// TODO Ability to create different SPI devices
		return new TestMcp3008SpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws IOException {
		// TODO Support for test I2C devices
		throw new UnsupportedOperationException("I2C not yet supported in the Test device provider factory");
	}
}
