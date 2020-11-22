package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SerialDeviceInterface.java  
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

import com.diozero.util.RuntimeIOException;

public interface SerialDeviceInterface extends DeviceInterface {
	/**
	 * Read a single byte returning error responses
	 * 
	 * @return Signed integer representation of the data read, including error
	 *         responses (values &lt; 0)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	int read() throws RuntimeIOException;

	/**
	 * Read a single byte, throw an exception if unable to read any data
	 * 
	 * @return The data read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	byte readByte() throws RuntimeIOException;

	/**
	 * Write a single byte, throw an exception if unable to write the data
	 * 
	 * @param bVal The data to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeByte(byte bVal) throws RuntimeIOException;

	/**
	 * Attempt to read buffer.length bytes into the specified buffer, throw an
	 * exception if unable to read any data
	 * 
	 * @param buffer The buffer to read into, the length of this buffer specifies
	 *               the number of bytes to read
	 * @return The number of bytes read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	int read(byte[] buffer) throws RuntimeIOException;

	/**
	 * Write the byte buffer to the device
	 * 
	 * @param data The data to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void write(byte[] data) throws RuntimeIOException;

	/**
	 * Get the number of bytes that are available to be read
	 *
	 * @return The number of bytes that are available to read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	int bytesAvailable() throws RuntimeIOException;
}
