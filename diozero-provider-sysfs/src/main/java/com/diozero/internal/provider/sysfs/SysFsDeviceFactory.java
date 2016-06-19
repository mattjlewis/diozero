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
import com.diozero.internal.spi.*;
import com.diozero.internal.spi.GpioDeviceInterface.Direction;
import com.diozero.util.RuntimeIOException;

public class SysFsDeviceFactory extends BaseNativeDeviceFactory {
	private static final String GPIO_ROOT_DIR = "/sys/class/gpio";
	private static final String EXPORT_FILE = "export";
	private static final String UNEXPORT_FILE = "unexport";
	private static final String GPIO_DIR_PREFIX = "gpio";
	private static final String DIRECTION_FILE = "direction";
	
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
		throw new UnsupportedOperationException("PWM not supported");
	}

	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		throw new UnsupportedOperationException("PWM not supported");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		export(pinNumber, Direction.INPUT);
		
		return new SysFsGpioInputDevice(this, getGpioDir(pinNumber), key, pinNumber, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws RuntimeIOException {
		export(pinNumber, Direction.OUTPUT);
		
		return new SysFsGpioOutputDevice(this, getGpioDir(pinNumber), key, pinNumber, initialValue);
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analogue input not supported");
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber, float initialValue,
			PwmType pwmType) throws RuntimeIOException {
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
		throw new UnsupportedOperationException("I2C not supported");
	}
	
	private void export(int pinNumber, Direction direction) {
		if (! isExported(pinNumber)) {
			try (Writer export_writer = new FileWriter(rootPath.resolve(EXPORT_FILE).toFile())) {
				export_writer.write(String.valueOf(pinNumber));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		
		Path direction_file = getGpioDir(pinNumber).resolve(DIRECTION_FILE);
		// TODO Do I need to do any of this polling?
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
			writer.write(direction == Direction.OUTPUT ? "out" : "in");
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
