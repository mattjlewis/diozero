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

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.board.beaglebone.BeagleBoneBoardInfoProvider;
import com.diozero.internal.board.chip.CHIPBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.spi.*;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SoftwarePwmOutputDevice;

public class SysFsDeviceFactory extends BaseNativeDeviceFactory {
	private static final String GPIO_ROOT_DIR = "/sys/class/gpio";
	private static final String EXPORT_FILE = "export";
	private static final String UNEXPORT_FILE = "unexport";
	private static final String GPIO_DIR_PREFIX = "gpio";
	private static final String DIRECTION_FILE = "direction";
	private static final int DEFAULT_PWM_FREQUENCY = 100;
	
	private Path rootPath;
	
	public SysFsDeviceFactory() {
		rootPath = FileSystems.getDefault().getPath(GPIO_ROOT_DIR);
	}

	/**
	 * Check if this pin is exported by checking the existance of /sys/class/gpio/gpioxxx/
	 * @param gpio GPIO pin
	 * @return Returns true if this pin is currently exported
	 */
	public boolean isExported(int gpio) {
		return Files.isDirectory(getGpioDir(gpio));
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int gpio) {
		if (boardInfo.sameMakeAndModel(OdroidBoardInfoProvider.ODROID_C2)) {
			return OdroidC2SysFsPwmOutputDevice.getFrequency(gpio);
		}
		
		throw new UnsupportedOperationException("PWM not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + boardInfo.getName() + "'");
	}

	@Override
	public void setPwmFrequency(int gpio, int pwmFrequency) {
		if (boardInfo.sameMakeAndModel(OdroidBoardInfoProvider.ODROID_C2)) {
			OdroidC2SysFsPwmOutputDevice.setFrequency(gpio, pwmFrequency);
			return;
		}
		
		throw new UnsupportedOperationException("PWM not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + boardInfo.getName() + "'");
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int gpio) throws RuntimeIOException {
		// TODO Analog input support on Odroid C2, Beaglebone Black and C.H.I.P.
		
		throw new UnsupportedOperationException("Analog input not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + boardInfo.getName() + "'");
	}

	@Override
	protected GpioAnalogOutputDeviceInterface createAnalogOutputPin(String key, int gpio) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog output not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + boardInfo.getName() + "'");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		export(gpio, Mode.DIGITAL_INPUT);
		
		return new SysFsDigitalInputDevice(this, getGpioDir(gpio), key, gpio, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int gpio, boolean initialValue)
			throws RuntimeIOException {
		export(gpio, Mode.DIGITAL_OUTPUT);
		
		return new SysFsDigitalOutputDevice(this, getGpioDir(gpio), key, gpio, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputPin(
			String key, int gpio, GpioDeviceInterface.Mode mode)
			throws RuntimeIOException {
		return new SysFsDigitalInputOutputDevice(this, getGpioDir(gpio), key, gpio, mode);
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int gpio, float initialValue,
			PwmType pwmType) throws RuntimeIOException {
		if (boardInfo.sameMakeAndModel(OdroidBoardInfoProvider.ODROID_C2)) {
			// FIXME Match with previously set PWM frequency...
			return new OdroidC2SysFsPwmOutputDevice(key, this, gpio, DEFAULT_PWM_FREQUENCY, initialValue);
		}
		if (boardInfo.sameMakeAndModel(BeagleBoneBoardInfoProvider.BBB_BOARD_INFO)) {
			// FIXME Match with previously set PWM frequency...
			return new BbbPwmSysFsPwmOutputDevice(key, this, gpio, DEFAULT_PWM_FREQUENCY, initialValue);
		}
		if (boardInfo.sameMakeAndModel(CHIPBoardInfoProvider.CHIP_BOARD_INFO) && gpio == 0) {
			// FIXME Match with previously set PWM frequency...
			return new ChipSysFsPwmOutputDevice(key, this, gpio, DEFAULT_PWM_FREQUENCY, initialValue);
		}
		SoftwarePwmOutputDevice pwm = new SoftwarePwmOutputDevice(key, this, createDigitalOutputPin(key, gpio, false), DEFAULT_PWM_FREQUENCY, initialValue);
		pwm.start();
		return pwm;
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException {
		return new SysFsSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new SysFsI2cDevice(this, key, controller, address, addressSize, clockFrequency);
	}
	
	void export(int gpio, Mode mode) {
		if (mode != Mode.DIGITAL_INPUT && mode != Mode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Illegal mode (" + mode +
					") must be " + Mode.DIGITAL_INPUT + " or " + Mode.DIGITAL_OUTPUT);
		}
		
		Logger.debug("export({}, {})", Integer.valueOf(gpio), mode);
		
		if (! isExported(gpio)) {
			try (Writer export_writer = new FileWriter(rootPath.resolve(EXPORT_FILE).toFile())) {
				export_writer.write(String.valueOf(gpio));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		
		Path direction_file = getGpioDir(gpio).resolve(DIRECTION_FILE);
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
				unexport(gpio);
				throw new RuntimeIOException("Waited for over " + delay + " ms for the GPIO pin to be created, aborting");
			}
		}

		// Defaults to in on most boards
		try (FileWriter writer = new FileWriter(direction_file.toFile(), true)) {
			writer.write(mode == Mode.DIGITAL_OUTPUT ? "out" : "in");
		} catch (IOException e) {
			unexport(gpio);
			throw new RuntimeIOException("Error setting direction for GPIO " + gpio, e);
		}
	}
	
	private Path getGpioDir(int gpio) {
		return rootPath.resolve(GPIO_DIR_PREFIX + gpio);
	}

	void unexport(int gpio) {
		if (isExported(gpio)) {
			try (Writer unexport_writer = new FileWriter(rootPath.resolve(UNEXPORT_FILE).toFile())) {
				unexport_writer.write(String.valueOf(gpio));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
}
