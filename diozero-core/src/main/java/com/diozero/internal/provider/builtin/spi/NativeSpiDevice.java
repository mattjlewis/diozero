package com.diozero.internal.provider.builtin.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     NativeSpiDevice.java  
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


import java.io.Closeable;

import org.tinylog.Logger;

import com.diozero.api.SpiConstants;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.util.LibraryLoader;

public class NativeSpiDevice implements Closeable {
	static {
		LibraryLoader.loadSystemUtils();
	}
	
	private static native int spiOpen(String filename, byte mode, int speedHz, byte bitsPerWord, boolean lsbFirst);
	private static native int spiConfig(int fileDescriptor, byte spiMode, int frequency, byte bitsPerWord, boolean lsbFirst);
	private static native int spiClose(int fileDescriptor);
	private static native int spiTransfer(int fileDescriptor, byte[] txBuffer, int txOffset,
			byte[] rxBuffer, int length, int speedHz, int delayUSecs, byte bitsPerWord, boolean csChange);
	
	private int controller;
	private int chipSelect;
	private int speedHz;
	private byte spiMode;
	private byte bitsPerWord;
	private int fd;

	public NativeSpiDevice(int controller, int chipSelect, int speedHz, SpiClockMode mode, boolean lsbFirst) {
		this.controller = controller;
		this.chipSelect = chipSelect;
		this.speedHz = speedHz;
		this.spiMode = mode.getMode();
		bitsPerWord = SpiConstants.DEFAULT_WORD_LENGTH;
		String spidev = "/dev/spidev" + controller + "." + chipSelect;
		
		Logger.trace("Opening {}, frequency {} Hz, mode {}", spidev, Integer.valueOf(speedHz), mode);
		fd = spiOpen(spidev, spiMode, speedHz, bitsPerWord, lsbFirst);
	}
	
	@Override
	public void close() {
		spiClose(fd);
	}
	
	public void write(byte[] txBuffer, int delayUSecs) {
		write(txBuffer, delayUSecs, false);
	}
	
	public void write(byte[] txBuffer, int delayUSecs, boolean csChange) {
		int rc = spiTransfer(fd, txBuffer, 0, null, txBuffer.length, speedHz, delayUSecs, bitsPerWord, csChange);
		if (rc < 0) {
			throw new RuntimeIOException("Error in spiTransfer(), response: " + rc);
		}
	}
	
	public void write(byte[] txBuffer, int txOffset, int length, int delayUSecs) {
		write(txBuffer, txOffset, length, delayUSecs, false);
	}
	
	public void write(byte[] txBuffer, int txOffset, int length, int delayUSecs, boolean csChange) {
		int rc = spiTransfer(fd, txBuffer, txOffset, null, length, speedHz, delayUSecs, bitsPerWord, csChange);
		if (rc < 0) {
			throw new RuntimeIOException("Error in spiTransfer(), response: " + rc);
		}
	}
	
	public byte[] writeAndRead(byte[] txBuffer, int delayUSecs) {
		return writeAndRead(txBuffer, delayUSecs, false);
	}
	
	public byte[] writeAndRead(byte[] txBuffer, int delayUSecs, boolean csChange) {
		byte[] rx = new byte[txBuffer.length];
		int rc = spiTransfer(fd, txBuffer, 0, rx, txBuffer.length, speedHz, delayUSecs, bitsPerWord, csChange);
		if (rc < 0) {
			throw new RuntimeIOException("Error in spiTransfer(), response: " + rc);
		}
		
		return rx;
	}
	
	public int getController() {
		return controller;
	}
	
	public int getChipSelect() {
		return chipSelect;
	}
}
