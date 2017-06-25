package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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


import java.io.Closeable;

import org.pmw.tinylog.Logger;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.RuntimeIOException;

/**
 * https://www.raspberrypi.org/documentation/hardware/raspberrypi/spi/README.md
 * For modern Raspberry Pis:
 * 2 SPI controllers, 0 (SPI-0) and 1 (SPI-1)
 * Controller 0 has 2 channels (CE-0 on physical pin 24, CE-1 on physical pin 26)
 * Controller 1 has 3 channels (CE-0 on physical pin 12, CE-1 on physical pin 11, CE-2 on physical pin 36)
 * SPI-1 is more limited that SPI-0 on the Raspberry Pi (https://www.raspberrypi.org/forums/viewtopic.php?t=81903&amp;p=579154)
 * - The SPI-1 clock is derived from the system clock therefore you have to be careful when over/underclocking to set the right divisor
 * - Limited IRQ support, no thresholding on the FIFO except "TX empty" or "done".
 * - No DMA support (no peripheral DREQ)
 */
public class SpiDevice implements Closeable, SPIConstants {
	private SpiDeviceInterface device;
	
	public SpiDevice(int chipSelect) throws RuntimeIOException {
		this(DEFAULT_SPI_CONTROLLER, chipSelect, DEFAULT_SPI_CLOCK_FREQUENCY, DEFAULT_SPI_CLOCK_MODE, DEFAULT_LSB_FIRST);
	}
	
	public SpiDevice(int controller, int chipSelect) throws RuntimeIOException {
		this(controller, chipSelect, DEFAULT_SPI_CLOCK_FREQUENCY, DEFAULT_SPI_CLOCK_MODE, DEFAULT_LSB_FIRST);
	}
	
	public SpiDevice(int controller, int chipSelect, int frequency, SpiClockMode mode, boolean lsbFirst) throws RuntimeIOException {
		device = DeviceFactoryHelper.getNativeDeviceFactory().provisionSpiDevice(controller, chipSelect, frequency, mode, lsbFirst);
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		device.close();
	}
	
	public int getController() {
		return device.getController();
	}
	
	public int getChipSelect() {
		return device.getChipSelect();
	}

	public void write(byte[] txBuffer) throws RuntimeIOException {
		device.write(txBuffer);
	}

	public void write(byte[] txBuffer, int txOffset, int length) throws RuntimeIOException {
		device.write(txBuffer, txOffset, length);
	}

	public byte[] writeAndRead(byte[] out) throws RuntimeIOException {
		return device.writeAndRead(out);
	}
}
