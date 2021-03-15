package com.diozero.internal.provider.builtin.spi;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiConstants;
import com.diozero.util.LibraryLoader;

public class NativeSpiDevice implements AutoCloseable {
	static {
		LibraryLoader.loadSystemUtils();
	}
	
	private static native int spiOpen(String filename, byte mode, int frequency, byte bitsPerWord, boolean lsbFirst);
	private static native int spiConfig(int fileDescriptor, byte spiMode, int frequency, byte bitsPerWord, boolean lsbFirst);
	private static native int spiClose(int fileDescriptor);
	private static native int spiTransfer(int fileDescriptor, byte[] txBuffer, int txOffset,
			byte[] rxBuffer, int length, int frequency, int delayUSecs, byte bitsPerWord, boolean csChange);
	
	private int controller;
	private int chipSelect;
	private int frequency;
	private byte spiMode;
	private byte bitsPerWord;
	private int fd;

	public NativeSpiDevice(int controller, int chipSelect, int frequency, SpiClockMode mode, boolean lsbFirst) {
		this.controller = controller;
		this.chipSelect = chipSelect;
		this.frequency = frequency;
		this.spiMode = mode.getMode();
		bitsPerWord = SpiConstants.DEFAULT_WORD_LENGTH;
		String spidev = "/dev/spidev" + controller + "." + chipSelect;
		
		Logger.trace("Opening {}, frequency {} Hz, mode {}", spidev, Integer.valueOf(frequency), mode);
		fd = spiOpen(spidev, spiMode, frequency, bitsPerWord, lsbFirst);
	}
	
	@Override
	public void close() {
		spiClose(fd);
	}
	
	public void write(byte[] txBuffer, int delayUSecs) {
		write(txBuffer, delayUSecs, false);
	}
	
	public void write(byte[] txBuffer, int delayUSecs, boolean csChange) {
		int rc = spiTransfer(fd, txBuffer, 0, null, txBuffer.length, frequency, delayUSecs, bitsPerWord, csChange);
		if (rc < 0) {
			throw new RuntimeIOException("Error in spiTransfer(), response: " + rc);
		}
	}
	
	public void write(byte[] txBuffer, int txOffset, int length, int delayUSecs) {
		write(txBuffer, txOffset, length, delayUSecs, false);
	}
	
	public void write(byte[] txBuffer, int txOffset, int length, int delayUSecs, boolean csChange) {
		int rc = spiTransfer(fd, txBuffer, txOffset, null, length, frequency, delayUSecs, bitsPerWord, csChange);
		if (rc < 0) {
			throw new RuntimeIOException("Error in spiTransfer(), response: " + rc);
		}
	}
	
	public byte[] writeAndRead(byte[] txBuffer, int delayUSecs) {
		return writeAndRead(txBuffer, delayUSecs, false);
	}
	
	public byte[] writeAndRead(byte[] txBuffer, int delayUSecs, boolean csChange) {
		byte[] rx = new byte[txBuffer.length];
		int rc = spiTransfer(fd, txBuffer, 0, rx, txBuffer.length, frequency, delayUSecs, bitsPerWord, csChange);
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
