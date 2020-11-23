package com.diozero.sbc;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     BoardInfo.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.internal.spi.MmapGpioInterface;

/**
 * Information about the connected SBC. Note that the connected board instance
 * might be a remote device, e.g. connected via serial, Bluetooth or TCP/IP. The
 * BoardInfo instance for the connected device must be obtained by calling
 * {@link com.diozero.internal.spi.NativeDeviceFactoryInterface#getBoardInfo()
 * getBoardInfo()} the on the
 * {@link com.diozero.internal.spi.NativeDeviceFactoryInterface
 * NativeDeviceFactoryInterface} instance returned from
 * {@link DeviceFactoryHelper#getNativeDeviceFactory()}.
 */
@SuppressWarnings("static-method")
public abstract class BoardInfo extends BoardPinInfo {
	public static final String UNKNOWN = "unknown";

	private static final float DEFAULT_ADC_VREF = 1.8f;
	private static final String TEMP_FILE = "/sys/class/thermal/thermal_zone0/temp";

	private String make;
	private String model;
	private int memoryKb;
	private String libraryPath;
	private float adcVRef;

	public BoardInfo(String make, String model, int memory, String libraryPath) {
		this(make, model, memory, libraryPath, DEFAULT_ADC_VREF);
	}

	public BoardInfo(String make, String model, int memory, String libraryPath, float adcVRef) {
		this.make = make;
		this.model = model;
		this.memoryKb = memory;
		this.libraryPath = libraryPath;
		this.adcVRef = adcVRef;
	}

	/**
	 * Pin initialisation is done separately to the constructor since all known
	 * BoardInfo instances get instantiated on startup by the Java ServiceLoader.
	 */
	public abstract void initialisePins();

	/**
	 * The make of the connected board, e.g. "Raspberry Pi"
	 * 
	 * @return the make of the connected board
	 */
	public String getMake() {
		return make;
	}

	/**
	 * The model of the connected board, e.g. "3 Model B+"
	 * 
	 * @return the model of the connected board
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Get the memory (in KB) of the connected board
	 * 
	 * @return memory in KB
	 */
	public int getMemoryKb() {
		return memoryKb;
	}

	/**
	 * Internal diozero method to get the library path prefix to be used when
	 * loading native libraries for this device.
	 * 
	 * @return the library path prefix
	 */
	public String getLibraryPath() {
		return libraryPath;
	}

	/**
	 * Get the Analog to Digital converter reference voltage to be used when taking
	 * ADC readings
	 * 
	 * @return the reference voltage for this board
	 */
	public float getAdcVRef() {
		return adcVRef;
	}

	/**
	 * Get the name of this board - usual a concatenation of make and model
	 * 
	 * @return the name of this board
	 */
	public String getName() {
		return make + " " + model;
	}

	/**
	 * Compare make and model
	 * 
	 * @param boardInfo the compare against
	 * @return true if the make and model are the same
	 */
	public boolean compareMakeAndModel(BoardInfo boardInfo) {
		return make.equals(boardInfo.getMake()) && model.equals(boardInfo.getModel());
	}

	/**
	 * Get the PWM chip for the specified PWM number. Only actually relevant for
	 * sysfs PWM control on the BeagleBone Black.
	 * 
	 * @param pwmNum The sysfs PWM channel number
	 * @return The PWM chip number for the requested PWM channel number, -1 if not
	 *         found / not relevant
	 */
	public int getPwmChip(int pwmNum) {
		return -1;
	}

	/**
	 * Instantiate the memory mapped GPIO interface for this board. Not that the
	 * caller needs to call {@link MmapGpioInterface#initialise initialise} prior to
	 * use.
	 * 
	 * @return the MMAP GPIO interface implementation for this board, null if there
	 *         isn't one
	 */
	public MmapGpioInterface createMmapGpio() {
		return null;
	}

	/**
	 * Utility method to get the CPU temperate of the attached board
	 * 
	 * @return the CPU temperature
	 */
	public float getCpuTemperature() {
		try {
			return Integer.parseInt(Files.lines(Paths.get(TEMP_FILE)).findFirst().orElse("0")) / 1000f;
		} catch (IOException e) {
			Logger.warn(e, "Error reading {}: {}", TEMP_FILE, e);
			return 0;
		}
	}

	/**
	 * Detect the I2C bus controller numbers that are available on this board.
	 * 
	 * @return collection of I2C bus controller numbers
	 */
	public Collection<Integer> getI2CBuses() {
		try {
			List<Integer> i2c_buses = new ArrayList<>();
			Files.newDirectoryStream(Paths.get("/dev"), "i2c-*")
					.forEach(path -> i2c_buses.add(Integer.valueOf(path.toString().split("-")[1])));
			return i2c_buses;
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
			return null;
		}
	}

	@Override
	public String toString() {
		return "BoardInfo [make=" + make + ", model=" + model + ", memory=" + memoryKb + ", libraryPath=" + libraryPath
				+ ", adcVRef=" + adcVRef + "]";
	}
}
