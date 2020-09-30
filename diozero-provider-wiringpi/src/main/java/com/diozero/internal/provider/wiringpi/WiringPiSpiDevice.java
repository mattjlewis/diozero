package com.diozero.internal.provider.wiringpi;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - wiringPi provider
 * Filename:     WiringPiSpiDevice.java  
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

import org.tinylog.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.wiringpi.Spi;

public class WiringPiSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final int CLOSED = -1;
	
	private int handle = CLOSED;
	private int controller;
	private int chipSelect;
	
	public WiringPiSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int chipSelect, int speed, SpiClockMode mode, boolean lsbFirst) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
		
		int wpi_spi_mode;
		switch (mode) {
		case MODE_0:
			wpi_spi_mode = Spi.MODE_0;
			break;
		case MODE_1:
			wpi_spi_mode = Spi.MODE_1;
			break;
		case MODE_2:
			wpi_spi_mode = Spi.MODE_2;
			break;
		case MODE_3:
		default:
			wpi_spi_mode = Spi.MODE_3;
			break;
		}
		
		handle = Spi.wiringPiSPISetupMode(controller, speed, wpi_spi_mode);
		if (handle == -1) {
			throw new RuntimeIOException("Error initialising SPI controller " + controller + ", speed=" + speed + ", mode=" + wpi_spi_mode);
		}
	}

	@Override
	public boolean isOpen() {
		return handle >= 0;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		// No way to close a wiringPi SPI device file handle?!
		handle = CLOSED;
	}

	@Override
	public void write(byte[] txBuffer) {
		if (Spi.wiringPiSPIDataRW(controller, txBuffer) == -1) {
			throw new RuntimeIOException("Error writing " + txBuffer.length + " bytes of data to SPI controller " + controller);
		}
	}
	
	@Override
	public void write(byte[] txBuffer, int offset, int length) {
		byte[] tx;
		if (offset == 0 && txBuffer.length == length) {
			tx = txBuffer;
		} else {
			tx = new byte[length];
			System.arraycopy(txBuffer, offset, tx, 0, length);
		}
		if (Spi.wiringPiSPIDataRW(controller, tx) == -1) {
			throw new RuntimeIOException("Error writing " + tx.length + " bytes of data to SPI controller " + controller);
		}
	}

	@Override
	public byte[] writeAndRead(byte[] txBuffer) throws RuntimeIOException {
		byte[] buffer = new byte[txBuffer.length];
		// Copy so that txBuffer isn't modified
		System.arraycopy(txBuffer, 0, buffer, 0, txBuffer.length);
		if (Spi.wiringPiSPIDataRW(controller, buffer) == -1) {
			throw new RuntimeIOException("Error writing " + buffer.length + " bytes of data to SPI controller " + controller);
		}
		
		return buffer;
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
