package com.diozero.internal.spi;

/*
 * #%L
 * Device I/O Zero - Core
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

import java.util.ArrayList;
import java.util.List;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.SpiClockMode;
import com.diozero.util.*;

/**
 * Helper class for instantiating different devices via the configured provider.
 * To set the provider edit META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface
 * While the ServiceLoader supports multiple service providers, only the first entry in this file is used
 */

public abstract class BaseNativeDeviceFactory extends AbstractDeviceFactory implements NativeDeviceFactoryInterface {
	private static final String NATIVE_PREFIX = "Native";
	private static final String I2C_PREFIX = NATIVE_PREFIX + "-I2C-";
	private static final String SPI_PREFIX = NATIVE_PREFIX + "-SPI-";
	
	private static String createI2CKey(int controller, int address) {
		return I2C_PREFIX + controller + "-" + address;
	}
	
	private static String createSpiKey(int controller, int chipSelect) {
		return SPI_PREFIX + controller + "-" + chipSelect;
	}
	
	private List<DeviceFactoryInterface> deviceFactories = new ArrayList<>();
	protected BoardInfo boardInfo;
	
	public BaseNativeDeviceFactory() {
		super(NATIVE_PREFIX);
		
		boardInfo = SystemInfo.getBoardInfo();
	}
	
	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardInfo;
	}
	
	@Override
	public float getVRef() {
		return boardInfo.getAdcVRef();
	}
	
	@Override
	public final void registerDeviceFactory(DeviceFactoryInterface deviceFactory) {
		deviceFactories.add(deviceFactory);
	}
	
	@Override
	public void shutdown() {
		for (DeviceFactoryInterface df : deviceFactories) {
			if (! df.isShutdown()) {
				df.shutdown();
			}
		}
		super.shutdown();
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
