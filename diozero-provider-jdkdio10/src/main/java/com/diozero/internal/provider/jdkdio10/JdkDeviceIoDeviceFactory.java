package com.diozero.internal.provider.jdkdio10;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - JDK Device I/O v1.0 provider
 * Filename:     JdkDeviceIoDeviceFactory.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.SerialDevice;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.BaseNativeDeviceFactory;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class JdkDeviceIoDeviceFactory extends BaseNativeDeviceFactory {
	public JdkDeviceIoDeviceFactory() {
		// Prevent having to reference a local GPIO policy file via
		// -Djava.security.policy=<policy-file> command line
		if (System.getSecurityManager() == null) {
			try {
				Path policy_file_path = Files.createTempFile("diozero-gpio-", ".policy");
				policy_file_path.toFile().deleteOnExit();
				try (InputStream is = getClass().getResourceAsStream("/config/gpio.policy")) {
					Files.copy(is, policy_file_path, StandardCopyOption.REPLACE_EXISTING);
				}
				System.setProperty("java.security.policy", policy_file_path.toString());
			} catch (IOException e) {
				Logger.error(e, "Error initialising JDK Device I/O security policy: {}", e);
			}
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName() + "10";
	}
	
	@Override
	public void shutdown() {
		// Ignore
	}

	@Override
	public int getBoardPwmFrequency() {
		throw new UnsupportedOperationException("PWM output isn't supported by JDK Device I/O");
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		throw new UnsupportedOperationException("PWM output isn't supported by JDK Device I/O");
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new JdkDeviceIoDigitalInputDevice(key, this, pinInfo.getDeviceNumber(), pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new JdkDeviceIoGpioOutputDevice(key, this, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		throw new UnsupportedOperationException("Digital Input / Output devices not yet supported by this provider");
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		throw new UnsupportedOperationException("PWM output isn't supported by JDK Device I/O");
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue)
			throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new JdkDeviceIoSpiDevice(key, this, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize)
			throws RuntimeIOException {
		return new JdkDeviceIoI2CDevice(key, this, controller, address, addressSize);
	}

	@Override
	public SerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud, SerialDevice.DataBits dataBits,
			SerialDevice.StopBits stopBits, SerialDevice.Parity parity, boolean readBlocking, int minReadChars,
			int readTimeoutMillis) throws RuntimeIOException {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}
}
