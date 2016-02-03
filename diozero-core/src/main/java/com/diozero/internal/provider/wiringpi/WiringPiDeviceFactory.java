package com.diozero.internal.provider.wiringpi;

import java.io.IOException;

import com.diozero.api.*;
import com.diozero.internal.spi.*;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDeviceFactory extends BaseNativeDeviceFactory {
	public WiringPiDeviceFactory() {
		// Initialise using native pin numbering scheme
		int status = Gpio.wiringPiSetupGpio();
		if (status != 0) {
			throw new RuntimeException("Error initialising wiringPi: " + status);
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws IOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new IOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new WiringPiDigitalInputDevice(key, this, pinNumber, pud, trigger);
	}

	@Override
	public GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws IOException {
		throw new UnsupportedOperationException("Analogue devices aren't supported on this device");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws IOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new IOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new WiringPiDigitalOutputDevice(key, this, pinNumber, initialValue);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws IOException {
		if (GpioUtil.isPinSupported(pinNumber) != 1) {
			throw new IOException("Error: Pin " + pinNumber + " isn't supported");
		}
		
		return new WiringPiPwmOutputDevice(key, this, pinNumber, initialValue, pwmType);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws IOException {
		return new WiringPiSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency)
			throws IOException {
		return new WiringPiI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
