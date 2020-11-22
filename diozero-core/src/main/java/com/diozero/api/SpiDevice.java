package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SpiDevice.java  
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

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

/**
 * https://www.raspberrypi.org/documentation/hardware/raspberrypi/spi/README.md
 * For modern Raspberry Pis: 2 SPI controllers, 0 (SPI-0) and 1 (SPI-1)
 * Controller 0 has 2 channels (CE-0 on physical pin 24, CE-1 on physical pin
 * 26) Controller 1 has 3 channels (CE-0 on physical pin 12, CE-1 on physical
 * pin 11, CE-2 on physical pin 36) SPI-1 is more limited that SPI-0 on the
 * Raspberry Pi
 * (https://www.raspberrypi.org/forums/viewtopic.php?t=81903&amp;p=579154) - The
 * SPI-1 clock is derived from the system clock therefore you have to be careful
 * when over/underclocking to set the right divisor - Limited IRQ support, no
 * thresholding on the FIFO except "TX empty" or "done". - No DMA support (no
 * peripheral DREQ)
 * 
 * On a RPi 3 you have to change the GPU core frequency to 250 MHz, otherwise
 * the SPI clock has the wrong frequency. Do this by adding the following line
 * to /boot/config.txt and reboot. core_freq=250
 */
public class SpiDevice implements SpiDeviceInterface, SpiConstants {
	private SpiDeviceInterface delegate;
	private int maxBufferSize;

	public SpiDevice(int chipSelect) throws RuntimeIOException {
		this(DEFAULT_SPI_CONTROLLER, chipSelect, DEFAULT_SPI_CLOCK_FREQUENCY, DEFAULT_SPI_CLOCK_MODE,
				DEFAULT_LSB_FIRST);
	}

	public SpiDevice(int controller, int chipSelect) throws RuntimeIOException {
		this(controller, chipSelect, DEFAULT_SPI_CLOCK_FREQUENCY, DEFAULT_SPI_CLOCK_MODE, DEFAULT_LSB_FIRST);
	}

	public SpiDevice(int controller, int chipSelect, int frequency, SpiClockMode mode, boolean lsbFirst)
			throws RuntimeIOException {
		NativeDeviceFactoryInterface ndf = DeviceFactoryHelper.getNativeDeviceFactory();
		delegate = ndf.provisionSpiDevice(controller, chipSelect, frequency, mode, lsbFirst);
		maxBufferSize = ndf.getSpiBufferSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getKey() {
		return delegate.getKey();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		delegate.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getController() {
		return delegate.getController();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getChipSelect() {
		return delegate.getChipSelect();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte[] txBuffer) throws RuntimeIOException {
		int written = 0;
		do {
			int to_write = Math.min(txBuffer.length - written, maxBufferSize);
			delegate.write(txBuffer, written, to_write);
			written += to_write;
		} while (written < txBuffer.length);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte[] txBuffer, int txOffset, int length) throws RuntimeIOException {
		delegate.write(txBuffer, txOffset, length);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] writeAndRead(byte[] out) throws RuntimeIOException {
		return delegate.writeAndRead(out);
	}
}
