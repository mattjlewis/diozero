package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     SysFsDeviceFactory.java  
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


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.PwmPinInfo;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.SoftwarePwmOutputDevice;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidC2SysFsPwmOutputDevice;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.BaseNativeDeviceFactory;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.EpollNative;
import com.diozero.util.RuntimeIOException;

public class SysFsDeviceFactory extends BaseNativeDeviceFactory {
	private static final String GPIO_ROOT_DIR = "/sys/class/gpio";
	private static final String EXPORT_FILE = "export";
	private static final String UNEXPORT_FILE = "unexport";
	private static final String GPIO_DIR_PREFIX = "gpio";
	private static final String DIRECTION_FILE = "direction";
	
	private Path rootPath;
	private int boardPwmFrequency;
	private EpollNative epoll;
	
	public SysFsDeviceFactory() {
		rootPath = FileSystems.getDefault().getPath(GPIO_ROOT_DIR);
	}
	
	@Override
	public void close() {
		if (epoll != null) {
			epoll.close();
		}
		super.close();
	}

	/**
	 * Check if this pin is exported by checking the existance of /sys/class/gpio/gpioxxx/
	 * @param gpio GPIO pin
	 * @return Returns true if this pin is currently exported
	 */
	public boolean isExported(int gpio) {
		return Files.isDirectory(getGpioDirectoryPath(gpio));
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		boardPwmFrequency = pwmFrequency;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new SysFsDigitalInputDevice(this, key, pinInfo, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new SysFsDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(
			String key, PinInfo pinInfo, DeviceMode mode)
			throws RuntimeIOException {
		return new SysFsDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		if (pinInfo instanceof PwmPinInfo) {
			PwmPinInfo pwm_pin_info = (PwmPinInfo) pinInfo;
			// Odroid C2 runs with an older 3.x kernel hence has a different sysfs interface
			if (getBoardInfo().sameMakeAndModel(OdroidBoardInfoProvider.ODROID_C2)) {
				return new OdroidC2SysFsPwmOutputDevice(key, this, pwm_pin_info, pwmFrequency, initialValue);
			}

			return new SysFsPwmOutputDevice(key, this, getBoardInfo().getPwmChip(pwm_pin_info.getPwmNum()), pwm_pin_info,
					pwmFrequency, initialValue);
		}

		SoftwarePwmOutputDevice pwm = new SoftwarePwmOutputDevice(key, this,
				createDigitalOutputDevice(createPinKey(pinInfo), pinInfo, false), pwmFrequency, initialValue);
		return pwm;
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo)
			throws RuntimeIOException {
		// TODO How to work out the device number?
		int device = 0;
		return new SysFsAnalogInputDevice(this, key, device, pinInfo.getDeviceNumber());
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo)
			throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog output not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + getBoardInfo().getName() + "'");
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new SysFsSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new SysFsI2CDevice(this, key, controller, address, addressSize, clockFrequency);
	}
	
	void export(int gpio, DeviceMode mode) {
		if (! isExported(gpio)) {
			try (Writer export_writer = new FileWriter(rootPath.resolve(EXPORT_FILE).toFile())) {
				export_writer.write(String.valueOf(gpio));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		
		Path direction_file = getGpioDirectoryPath(gpio).resolve(DIRECTION_FILE);
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

		// Defaults to "in" on all boards I have encountered
		try (FileWriter writer = new FileWriter(direction_file.toFile(), true)) {
			writer.write(mode == DeviceMode.DIGITAL_OUTPUT ? "out" : "in");
		} catch (IOException e) {
			unexport(gpio);
			throw new RuntimeIOException("Error setting direction for GPIO " + gpio, e);
		}
	}
	
	Path getGpioDirectoryPath(int gpio) {
		return rootPath.resolve(GPIO_DIR_PREFIX + gpio);
	}

	void unexport(int gpio) {
		if (isExported(gpio)) {
			try (Writer unexport_writer = new FileWriter(rootPath.resolve(UNEXPORT_FILE).toFile())) {
				unexport_writer.write(String.valueOf(gpio));
			} catch (IOException e) {
				// Issue #27, BBB throws an IOException: Invalid argument when closing
				if (!e.getMessage().equalsIgnoreCase("Invalid argument")) {
					throw new RuntimeIOException(e);
				}
			}
		}
	}
	
	synchronized EpollNative getEpoll() {
		// Has to be lazy loaded as cannot call System.loadLibrary within device factory initialisation
		if (epoll == null) {
			epoll = new EpollNative();
			epoll.enableEvents();
		}
		return epoll;
	}
}
