package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Java Sysfs provider
 * %%
 * Copyright (C) 2016 mattjlewis
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


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.diozero.api.*;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.spi.*;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SystemInfo;

public class SysFsDeviceFactory extends BaseNativeDeviceFactory {
	private static final String GPIO_ROOT_DIR = "/sys/class/gpio";
	private static final String EXPORT_FILE = "export";
	private static final String UNEXPORT_FILE = "unexport";
	private static final String GPIO_DIR_PREFIX = "gpio";
	private static final String DIRECTION_FILE = "direction";
	private static final int DEFAULT_PWM_FREQUENCY = 100_000;
	
	private Path rootPath;
	
	public SysFsDeviceFactory() {
		rootPath = FileSystems.getDefault().getPath(GPIO_ROOT_DIR);
	}

	/**
	 * Check if this pin is exported by checking the existance of /sys/class/gpio/gpioxxx/
	 * @param pinNumber GPIO pin
	 * @return Returns true if this pin is currently exported
	 */
	public boolean isExported(int pinNumber) {
		return Files.isDirectory(getGpioDir(pinNumber));
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int pinNumber) {
		if (SystemInfo.getBoardInfo().sameMakeAndModel(OdroidBoardInfoProvider.ORDOID_C2)) {
			return OdroidC2SysFsPwmOutputDevice.getFrequency(pinNumber);
		}
		
		throw new UnsupportedOperationException("PWM not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + SystemInfo.getBoardInfo() + "'");
	}

	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		if (SystemInfo.getBoardInfo().sameMakeAndModel(OdroidBoardInfoProvider.ORDOID_C2)) {
			OdroidC2SysFsPwmOutputDevice.setFrequency(pinNumber, pwmFrequency);
			return;
		}
		
		throw new UnsupportedOperationException("PWM not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + SystemInfo.getBoardInfo() + "'");
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		// TODO Analog input support on Odroid C2
		
		throw new UnsupportedOperationException("Analog input not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + SystemInfo.getBoardInfo() + "'");
	}

	@Override
	protected GpioAnalogOutputDeviceInterface createAnalogOutputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog output not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + SystemInfo.getBoardInfo() + "'");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		export(pinNumber, Mode.DIGITAL_INPUT);
		
		return new SysFsDigitalInputDevice(this, getGpioDir(pinNumber), key, pinNumber, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws RuntimeIOException {
		export(pinNumber, Mode.DIGITAL_OUTPUT);
		
		return new SysFsDigitalOutputDevice(this, getGpioDir(pinNumber), key, pinNumber, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputPin(
			String key, int pinNumber, GpioDeviceInterface.Mode mode)
			throws RuntimeIOException {
		return new SysFsDigitalInputOutputDevice(this, getGpioDir(pinNumber), key, pinNumber, mode);
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber, float initialValue,
			PwmType pwmType) throws RuntimeIOException {
		if (SystemInfo.getBoardInfo().sameMakeAndModel(OdroidBoardInfoProvider.ORDOID_C2)) {
			// FIXME Match with previously set PWM frequency...
			return new OdroidC2SysFsPwmOutputDevice(key, this, pinNumber, DEFAULT_PWM_FREQUENCY, initialValue, 1023);
		}
		throw new UnsupportedOperationException("PWM not supported");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException {
		throw new UnsupportedOperationException("SPI not supported");
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new SysFsI2cDevice(this, key, controller, address, addressSize, clockFrequency);
	}
	
	void export(int pinNumber, Mode mode) {
		if (mode != Mode.DIGITAL_INPUT && mode != Mode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Illegal mode (" + mode +
					") must be " + Mode.DIGITAL_INPUT + " or " + Mode.DIGITAL_OUTPUT);
		}

		if (! isExported(pinNumber)) {
			try (Writer export_writer = new FileWriter(rootPath.resolve(EXPORT_FILE).toFile())) {
				export_writer.write(String.valueOf(pinNumber));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		
		Path direction_file = getGpioDir(pinNumber).resolve(DIRECTION_FILE);
		// TODO Is this polling actually required?
		// Wait up to 500ms for the gpioxxx/direction file to exist
		int delay = 500;
		long start = System.currentTimeMillis();
		while (! Files.isWritable(direction_file)) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
				// Ignore
			}
			if (System.currentTimeMillis() - start > delay) {
				unexport(pinNumber);
				throw new RuntimeIOException("Waited for over " + delay + " ms for the GPIO pin to be created, aborting");
			}
		}

		// Defaults to in on the Raspberry Pi
		try (FileWriter writer = new FileWriter(direction_file.toFile(), true)) {
			writer.write(mode == Mode.DIGITAL_OUTPUT ? "out" : "in");
		} catch (IOException e) {
			unexport(pinNumber);
			throw new RuntimeIOException("Error setting direction for GPIO " + pinNumber, e);
		}
	}
	
	private Path getGpioDir(int pinNumber) {
		return rootPath.resolve(GPIO_DIR_PREFIX + pinNumber);
	}

	void unexport(int pinNumber) {
		if (isExported(pinNumber)) {
			try (Writer unexport_writer = new FileWriter(rootPath.resolve(UNEXPORT_FILE).toFile())) {
				unexport_writer.write(String.valueOf(pinNumber));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
}
