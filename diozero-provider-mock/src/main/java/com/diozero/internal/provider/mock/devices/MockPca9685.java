package com.diozero.internal.provider.mock.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Mock provider
 * Filename:     MockPca9685.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import org.tinylog.Logger;

import com.diozero.api.I2CDevice.ProbeMode;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.InternalI2CDeviceInterface;

/*-
 * Only need to implement these methods:
 * readByteData
 * writeByteData
 */
public class MockPca9685 implements InternalI2CDeviceInterface {
	private static final int NUM_CHANNELS = 16;
	private static final int RANGE = (int) Math.pow(2, 12);

	private static final int CLOCK_FREQ = 25_000_000; // 25MHz default osc clock

	private static final int MODE1 = 0x00;
	private static final int MODE2 = 0x01;

	private static final int LED0 = 0x06;
	private static final int LED0_ON_L = LED0;
	private static final int LED0_ON_H = 0x07;
	private static final int LED0_OFF_L = 0x08;
	private static final int LED0_OFF_H = 0x09;
	private static final int ALL_LED_ON_L = 0xFA; // Load all the LEDn_ON registers, byte 0 (turn 0-7 channels on)
	private static final int ALL_LED_ON_H = 0xFB; // Load all the LEDn_ON registers, byte 1 (turn 8-15 channels on)
	private static final int ALL_LED_OFF_L = 0xFC; // Load all the LEDn_OFF registers, byte 0 (turn 0-7 channels off)
	private static final int ALL_LED_OFF_H = 0xFD; // Load all the LEDn_OFF registers, byte 1 (turn 8-15 channels off)

	private static final int PRESCALE = 0xFE; // Prescaler for output frequency

	private String key;
	private boolean open;
	private byte prescale;
	private int[] onValues;
	private int[] offValues;

	public MockPca9685(String key) {
		this.key = key;
		open = true;
		onValues = new int[NUM_CHANNELS];
		offValues = new int[NUM_CHANNELS];
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public void close() throws RuntimeIOException {
		open = false;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public boolean isChild() {
		return false;
	}

	@Override
	public void setChild(boolean child) {
		// Ignore
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		Logger.debug("readByteData({})", Integer.valueOf(register));
		switch (register) {
		case MODE1:
			return 0;
		case PRESCALE:
			return prescale;
		default:
			Logger.warn("Unhandled register {}", Integer.valueOf(register));
			return 0;
		}
	}

	@Override
	public void writeByteData(int register, byte data) throws RuntimeIOException {
		Logger.debug("writeByteData({}, {})", Integer.valueOf(register), Byte.valueOf(data));
		switch (register) {
		case MODE1:
			// Ignore - used to put the device in sleep mode / restart it
			break;
		case MODE2:
			// Ignore - output driver mode
			break;
		case PRESCALE:
			prescale = data;
			float pwm_freq = (((float) CLOCK_FREQ) / RANGE / (prescale + 1f));
			Logger.debug("prescale: {}, pwm_freq: {}", Byte.valueOf(prescale), Float.valueOf(pwm_freq));
			break;
		default:
			if (register >= LED0_ON_L && register <= LED0_OFF_H + 4 * NUM_CHANNELS) {
				int channel = (register - LED0_ON_L) / 4;
				int part = (register - LED0_ON_L) % 4;
				switch (part) {
				case 0:
					// On lsb
					onValues[channel] = (onValues[channel] & 0xff00) | (data & 0xff);
					break;
				case 1:
					// On msb
					onValues[channel] = (onValues[channel] & 0x00ff) | ((data << 8) & 0xff00);
					Logger.debug("on[{}]: {}", Integer.valueOf(channel), Integer.valueOf(onValues[channel]));
					break;
				case 2:
					// Off lsb
					offValues[channel] = (offValues[channel] & 0xff00) | (data & 0xff);
					break;
				case 3:
					// Off msb
					offValues[channel] = (offValues[channel] & 0x00ff) | ((data << 8) & 0xff00);
					Logger.debug("off[{}]: {}", Integer.valueOf(channel), Integer.valueOf(offValues[channel]));
					break;
				default:
					// Not possible due to "% 4" above
					break;
				}
				Logger.info("channel: {}, part: {}", Integer.valueOf(channel), Integer.valueOf(part));
			} else {
				Logger.warn("Unhandled register {}", Integer.valueOf(register));
			}
		}
	}

	/*
	 * The following methods aren't invoked by PCA9695
	 */

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBytes(byte... data) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readWrite(I2CMessage[] messages, byte[] buffer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean probe(ProbeMode mode) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeQuick(byte bit) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeByte(byte data) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeWordData(int register, short data) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public short processCall(int register, short data) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] readBlockData(int register) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBlockData(int register, byte... data) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] blockProcessCall(int register, byte... txData) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeI2CBlockData(int register, byte... data) throws RuntimeIOException {
		throw new UnsupportedOperationException();
	}
}
