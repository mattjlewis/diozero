package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SpiDeviceInterface.java  
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

/**
 * <a href="https://en.wikipedia.org/wiki/Serial_Peripheral_Interface">Serial Peripheral Interface (SPI)</a>
 */
public interface SpiDeviceInterface extends DeviceInterface {
	/**
	 * Get the SPI controller
	 * 
	 * @return the SPI controller
	 */
	int getController();

	/**
	 * Get the SPI Chip Select
	 * 
	 * @return the SPI chip select
	 */
	int getChipSelect();

	/**
	 * Write the entire contents of <code>data</code> to the device
	 * 
	 * @param data the data to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void write(byte... data) throws RuntimeIOException;

	/**
	 * Write <code>length</code> bytes from <code>data</code> starting at
	 * <code>offset</code>
	 * 
	 * @param data   the data to write.
	 * @param offset the start offset in the data.
	 * @param length the number of bytes to write.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void write(byte[] data, int offset, int length) throws RuntimeIOException;

	/**
	 * Write the <code>data</code> to the device then read <code>data.length</code>
	 * bytes from the device
	 * 
	 * @param data the data to write.
	 * @return the data read from the device, same length as the data written.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	byte[] writeAndRead(byte[] data) throws RuntimeIOException;
}
