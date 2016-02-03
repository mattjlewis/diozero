package com.diozero.internal.provider.jdkdio11;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.diozero.api.*;
import com.diozero.internal.spi.*;

public class JdkDeviceIoDeviceFactory extends BaseNativeDeviceFactory {

	public JdkDeviceIoDeviceFactory() {
		// Prevent having to reference a local GPIO policy file via -Djava.security.policy=<policy-file> command line
		if (System.getSecurityManager() == null) {
			try {
				Path policy_file_path = Files.createTempFile("diozero-gpio-", ".policy");
				policy_file_path.toFile().deleteOnExit();
				try (InputStream is = getClass().getResourceAsStream("/config/gpio.policy")) {
					Files.copy(is, policy_file_path, StandardCopyOption.REPLACE_EXISTING);
				}
				System.setProperty("java.security.policy", policy_file_path.toString());
			} catch (IOException e) {
				// Ignore
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName() + "11";
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws IOException {
		return new JdkDeviceIoGpioInputDevice(key, this, pinNumber, pud, trigger);
	}

	@Override
	public GpioAnalogueInputDeviceInterface createAnalogueInputPin(String key, int pinNumber) throws IOException {
		throw new UnsupportedOperationException("Analogue devices aren't supported on this device");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws IOException {
		return new JdkDeviceIoGpioOutputDevice(key, this, pinNumber, initialValue);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws IOException {
		return new JdkDeviceIoPwmOutputDevice(key, this, pinNumber, initialValue, pwmType);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency, SpiClockMode spiClockMode) throws IOException {
		return new JdkDeviceIoSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws IOException {
		return new JdkDeviceIoI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
