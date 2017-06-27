package com.diozero.internal.provider;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     BaseNativeDeviceFactory.java  
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
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.SpiClockMode;
import com.diozero.util.*;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used
 */

public abstract class BaseNativeDeviceFactory extends AbstractDeviceFactory implements NativeDeviceFactoryInterface {
	private static final String NATIVE_PREFIX = "Native";
	private static final String I2C_PREFIX = NATIVE_PREFIX + "-I2C-";
	private static final String SPI_PREFIX = NATIVE_PREFIX + "-SPI-";
	private static final int DEFAULT_SPI_BUFFER_SIZE = 4096;
	
	private static String createI2CKey(int controller, int address) {
		return I2C_PREFIX + controller + "-" + address;
	}
	
	private static String createSpiKey(int controller, int chipSelect) {
		return SPI_PREFIX + controller + "-" + chipSelect;
	}
	
	private List<DeviceFactoryInterface> deviceFactories = new ArrayList<>();
	private BoardInfo boardInfo;
	
	public BaseNativeDeviceFactory() {
		super(NATIVE_PREFIX);
	}

	@SuppressWarnings("static-method")
	protected BoardInfo initialiseBoardInfo() {
		return SystemInfo.lookupLocalBoardInfo();
	}
	
	@Override
	public synchronized BoardInfo getBoardInfo() {
		if (boardInfo == null) {
			// Note this has been separated from the constructor to allow derived classes to
			// override default behaviour, in particular remote devices using e.g. Firmata protocol
			boardInfo = initialiseBoardInfo();
		}
		return boardInfo;
	}
	
	@Override
	public BoardPinInfo getBoardPinInfo() {
		return getBoardInfo();
	}
	
	@Override
	public float getVRef() {
		return boardInfo.getAdcVRef();
	}

	@Override
	public int getSpiBufferSize() {
		try {
			return Files.lines(Paths.get("/sys/module/spidev/parameters/bufsiz")).mapToInt(Integer::parseInt).findFirst()
					.orElse(DEFAULT_SPI_BUFFER_SIZE);
		} catch (IOException e) {
			Logger.warn("Error: {}", e);
			return DEFAULT_SPI_BUFFER_SIZE;
		}
	}
	
	@Override
	public final void registerDeviceFactory(DeviceFactoryInterface deviceFactory) {
		deviceFactories.add(deviceFactory);
	}
	
	@Override
	public void close() {
		// Stop all sheduled jobs
		DioZeroScheduler.shutdownAll();
		// Shutdown all of the other non-native device factories
		for (DeviceFactoryInterface df : deviceFactories) {
			if (! df.isClosed()) {
				df.close();
			}
		}
		// Now close all devices provisioned directly by this device factory
		super.close();
	}

	@Override
	public final SpiDeviceInterface provisionSpiDevice(int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		String key = createSpiKey(controller, chipSelect);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		SpiDeviceInterface device = createSpiDevice(key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public final I2CDeviceInterface provisionI2CDevice(int controller, int address, int addressSize, int clockFrequency) throws RuntimeIOException {
		String key = createI2CKey(controller, address);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		I2CDeviceInterface device = createI2CDevice(key, controller, address, addressSize, clockFrequency);
		deviceOpened(device);
		
		return device;
	}

	protected abstract SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException;
	protected abstract I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException;
}
