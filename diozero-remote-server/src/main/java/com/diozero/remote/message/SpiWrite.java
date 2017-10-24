package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     SpiWrite.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

public class SpiWrite extends SpiBase {
	private static final long serialVersionUID = -4902068267688229361L;
	
	private byte[] txData;

	public SpiWrite(int controller, int chipSelect, byte[] txData, String correlationId) {
		super(controller, chipSelect, correlationId);
		
		this.txData = txData;
	}

	public SpiWrite(int controller, int chipSelect, byte[] txData, int txOffset, int length, String correlationId) {
		super(controller, chipSelect, correlationId);

		if (txOffset == 0 && length == txData.length) {
			this.txData = txData;
		} else {
			this.txData = new byte[length];
			System.arraycopy(txData, txOffset, this.txData, 0, length);
		}
	}

	public byte[] getTxData() {
		return txData;
	}

	@Override
	public String toString() {
		return "SpiWrite [txData.length=" + txData.length + ", controller=" + getController() + ", chipSelect="
				+ getChipSelect() + "]";
	}
}
