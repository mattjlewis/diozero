package com.diozero.internal.provider.jdkdio10;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
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


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.diozero.api.*;
import com.diozero.internal.spi.*;
import com.diozero.util.RuntimeIOException;

public class JdkDeviceIoDeviceFactory extends BaseNativeDeviceFactory {

	public JdkDeviceIoDeviceFactory() {
		// Prevent having to reference a local GPIO policy file via -Djava.security.policy=<policy-file> command line
		if (System.getSecurityManager() == null) {
			try {
				Path policy_file_path = Files.createTempFile("dio-zero-gpio-", ".policy");
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
		return getClass().getSimpleName() + "10";
	}

	@Override
	public int getPwmFrequency(int pinNumber) {
		throw new UnsupportedOperationException("PWM output isn't supported by JDK Device I/O");
	}

	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		throw new UnsupportedOperationException("PWM output isn't supported by JDK Device I/O");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new JdkDeviceIoGpioInputDevice(key, this, pinNumber, pud, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue) throws RuntimeIOException {
		return new JdkDeviceIoGpioOutputDevice(key, this, pinNumber, initialValue);
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		throw new UnsupportedOperationException("PWM output isn't supported by JDK Device I/O");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency, SpiClockMode spiClockMode) throws RuntimeIOException {
		return new JdkDeviceIoSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		return new JdkDeviceIoI2CDevice(key, this, controller, address, addressSize, clockFrequency);
	}
}
