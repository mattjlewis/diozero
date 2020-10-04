package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     SerialBytesAvailableResponse.java  
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

public class SerialBytesAvailableResponse extends Response {
	private static final long serialVersionUID = -3807358219617878806L;

	private int bytesAvailable;
	
	public SerialBytesAvailableResponse(String error, String correlationId) {
		super(Status.ERROR, error, correlationId);
	}
	
	public SerialBytesAvailableResponse(int bytesAvailable, String correlationId) {
		this(Status.OK, null, bytesAvailable, correlationId);
	}
	
	public SerialBytesAvailableResponse(Response.Status status, String detail, int bytesAvailable, String correlationId) {
		super(status, detail, correlationId);
		
		this.bytesAvailable = bytesAvailable;
	}

	public int getBytesAvailable() {
		return bytesAvailable;
	}
}
