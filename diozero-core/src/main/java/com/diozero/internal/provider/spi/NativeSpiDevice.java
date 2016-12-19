package com.diozero.internal.provider.spi;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import com.diozero.api.SPIConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

/**
 * <p>Native Java implementation of the I2C SMBus commands using a single native method to select the slave address.</p>
 * <p>Reference <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel I2C dev interface</a>
 * and <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus Protocol</a>.</p>
 * <p><em>Warning</em> Not all methods have been tested!</p>
 */
public class NativeSpiDevice implements Closeable {
	static {
		LibraryLoader.loadLibrary(NativeSpiDevice.class, "diozero-system-utils");
	}
	
	private static final boolean LSB_FIRST = false;
	
	private static native int spiOpen(String filename, byte mode, int speedHz, byte bitsPerWord, boolean lsbFirst);
	private static native int spiConfig(int fileDescriptor, byte spiMode, int frequency, byte bitsPerWord, boolean lsbFirst);
	private static native int spiClose(int fileDescriptor);
	private static native int spiTransfer(int fileDescriptor, byte[] txBuffer,
			byte[] rxBuffer, int length, int speedHz, int delayUSecs, byte bitsPerWord);
	
	private RandomAccessFile i2cDeviceFile;
	private int controller;
	private int chipSelect;
	private int speedHz;
	private byte spiMode;
	private byte bitsPerWord;
	private int fd;

	public NativeSpiDevice(int controller, int chipSelect, int speedHz, SpiClockMode mode) {
		this.controller = controller;
		this.chipSelect = chipSelect;
		this.speedHz = speedHz;
		this.spiMode = mode.getMode();
		bitsPerWord = SPIConstants.DEFAULT_WORD_LENGTH;
		fd = spiOpen("/dev/spidev" + controller + "." + chipSelect, spiMode, speedHz, bitsPerWord, LSB_FIRST);
	}
	
	@Override
	public void close() {
		spiClose(fd);
	}
	
	public ByteBuffer transfer(ByteBuffer txBuffer, int delayUSecs) {
		int length = txBuffer.remaining();
		byte[] tx = new byte[length];
		txBuffer.get(tx);
		byte[] rx = new byte[length];
		int rc = spiTransfer(fd, tx, rx, length, speedHz, delayUSecs, bitsPerWord);
		if (rc < 0) {
			throw new RuntimeIOException("Error in spiTransfer(), response: " + rc);
		}
		
		return ByteBuffer.wrap(rx);
	}
	
	public int getController() {
		return controller;
	}
	
	public int getChipSelect() {
		return chipSelect;
	}
}
