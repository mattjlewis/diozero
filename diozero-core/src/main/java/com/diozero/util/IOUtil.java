package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     IOUtil.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class IOUtil {
	/**
	 * Attempt to read buffer.length bytes into buffer from the input steam. This
	 * implementation loops until the buffer is fully populated with data. The
	 * number of bytes actually read is returned - it may not equal buffer.length if
	 * the end of the input stream is reached.
	 * 
	 * @param is     the input stream to read from
	 * @param buffer the buffer to read into; the length of the buffer specifies the
	 *               number of bytes to read
	 * @return the number of bytes actually read, can be less than buffer.length if
	 *         EOF is reached
	 * @throws IOException if a read error occurs
	 */
	public static int read(final InputStream is, final byte[] buffer) throws IOException {
		int bytes_to_read = buffer.length;
		while (bytes_to_read > 0) {
			int read = is.read(buffer, buffer.length - bytes_to_read, bytes_to_read);
			if (read == -1) {
				// End of file reached
				break;
			}
			bytes_to_read -= read;
		}

		return buffer.length - bytes_to_read;
	}

	/**
	 * Attempt to read buffer.length bytes into buffer from the input steam, throw
	 * an exception if unable to read buffer.length bytes.
	 * 
	 * @param is     the input stream to read from
	 * @param buffer the buffer to read into; the length of the buffer specifies the
	 *               number of bytes to read
	 * @throws IOException  if a read error occurs
	 * @throws EOFException if unable to read buffer.length bytes
	 */
	public static void readFully(final InputStream is, final byte[] buffer) throws IOException, EOFException {
		int read = read(is, buffer);
		if (read != buffer.length) {
			throw new EOFException("End of file reached while attempting to read " + buffer.length
					+ " bytes, could only read " + read + " bytes");
		}
	}
}
