package com.diozero.internal.provider.pigpioj;

import java.io.IOException;

import com.diozero.api.*;
import com.diozero.internal.spi.*;

public class PigpioJDeviceFactory extends BaseNativeDeviceFactory {

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws IOException {
		return new PigpioJDigitalInputDevice(key, this, pinNumber, pud, trigger);
	}

	@Override
	protected GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws IOException {
		throw new UnsupportedOperationException("Analogue input pins not supported");
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws IOException {
		return new PigpioJDigitalOutputDevice(key, this, pinNumber, initialValue);
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber, float initialValue,
			PwmType pwmType) throws IOException {
		return new PigpioJPwmOutputDevice(key, this, pinNumber, initialValue);
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws IOException {
		throw new UnsupportedOperationException("SPI not yet supported");
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws IOException {
		throw new UnsupportedOperationException("I2C not yet supported");
	}

}
