package com.diozero.internal.provider.pi4j;

import java.io.IOException;

import com.diozero.api.*;
import com.diozero.internal.spi.*;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;

public class Pi4jDeviceFactory extends BaseNativeDeviceFactory {
	private GpioController gpioController;
	
	public Pi4jDeviceFactory() {
		gpioController = GpioFactory.getInstance();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws IOException {
		return new Pi4jGpioInputDevice(key, this, gpioController, pinNumber, pud, trigger);
	}

	@Override
	public GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws IOException {
		throw new UnsupportedOperationException("Analogue devices aren't supported on this device");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws IOException {
		return new Pi4jGpioOutputDevice(key, this, gpioController, pinNumber, initialValue);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws IOException {
		return new Pi4jPwmOutputDevice(key, this, gpioController, pinNumber, initialValue, pwmType);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency, SpiClockMode spiClockMode) throws IOException {
		return new Pi4jSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws IOException {
		return new Pi4jI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
