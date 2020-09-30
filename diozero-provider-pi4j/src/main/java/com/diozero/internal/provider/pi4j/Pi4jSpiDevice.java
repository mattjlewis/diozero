package com.diozero.internal.provider.pi4j;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - pi4j provider
 * Filename:     Pi4jSpiDevice.java  
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

import org.tinylog.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.io.spi.SpiMode;

public class Pi4jSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private final SpiDevice spiDevice;
	private int controller;
	private int chipSelect;
	
	public Pi4jSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int chipSelect, int speed, SpiClockMode mode, boolean lsbFirst) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
		try {
			spiDevice = SpiFactory.getInstance(SpiChannel.getByNumber(chipSelect), speed, SpiMode.getByNumber(mode.getMode()));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		// No way to close a Pi4J SPI Device?!
		//spiDevice.close();
	}
	
	@Override
	public void write(byte[] txBuffer) {
		try {
			spiDevice.write(txBuffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public void write(byte[] txBuffer, int offset, int length) {
		try {
			spiDevice.write(txBuffer, offset, length);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public byte[] writeAndRead(byte[] txBuffer) throws RuntimeIOException {
		try {
			return spiDevice.write(txBuffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public int getController() {
		return controller;
	}

	@Override
	public int getChipSelect() {
		return chipSelect;
	}
}
