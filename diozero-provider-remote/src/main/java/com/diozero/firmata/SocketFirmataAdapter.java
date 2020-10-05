package com.diozero.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     SocketFirmataAdapter.java  
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.diozero.util.RuntimeIOException;

public class SocketFirmataAdapter extends FirmataAdapter {
	private Socket socket;
	private InputStream is;
	private OutputStream os;

	public SocketFirmataAdapter(FirmataEventListener eventListener, String hostname, int port) throws IOException {
		super(eventListener);

		socket = new Socket(hostname, port);
		this.is = socket.getInputStream();
		this.os = socket.getOutputStream();

		connected();
	}

	@Override
	int read() throws RuntimeIOException {
		try {
			return is.read();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	byte readByte() throws RuntimeIOException {
		try {
			int i = is.read();
			if (i == -1) {
				throw new RuntimeIOException("End of stream unexpected");
			}
			return (byte) i;
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	void write(byte[] data) throws RuntimeIOException {
		try {
			os.write(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void close() {
		super.close();

		try {
			is.close();
		} catch (IOException e) {
		}
		try {
			os.close();
		} catch (IOException e) {
		}
		try {
			socket.close();
		} catch (IOException e) {
		}
	}
}
