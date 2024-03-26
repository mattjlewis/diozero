package com.diozero.devices.oled;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SsdOledCommunicationChannel.java
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.SpiDevice;
import com.diozero.api.SpiDeviceInterface;
import com.diozero.util.SleepUtil;

/**
 * Comms for OLED devices.
 */
public interface SsdOledCommunicationChannel extends AutoCloseable {
	/**
	 * Send to the device.
	 *
	 * @param buffer data to send
	 */
	void write(byte... buffer);

	/**
	 * Send parts to the device.
	 *
	 * @param buffer data to send
	 * @param offset offset
	 * @param length length
	 */
	void write(byte[] buffer, int offset, int length);

	@Override
	void close();

	/**
	 * Optionally, reset the device.
	 */
	default void reset() {
		//
	}

	/**
	 * Sends a "command".
	 *
	 * @param commands the set of commands to send
	 */
	void sendCommand(byte... commands);

	/**
	 * Sends a "data buffer".
	 *
	 * @param buffer the buffer
	 */
	void sendData(byte... buffer);

	/**
	 * Send part of a "data buffer"
	 *
	 * @param buffer the buffer
	 * @param offset offset
	 * @param length size
	 */
	void sendData(byte[] buffer, int offset, int length);

	/**
	 * SPI channel, with a data pin and a reset pin.
	 */
	class SpiCommunicationChannel implements SsdOledCommunicationChannel {
		public static final int SPI_FREQUENCY = 8_000_000;
		private final SpiDeviceInterface device;
		private final DigitalOutputDevice dcPin;
		private final DigitalOutputDevice resetPin;

		public SpiCommunicationChannel(int controller, int chipSelect, DigitalOutputDevice dcPin,
				DigitalOutputDevice resetPin) {
			this(SpiDevice.builder(chipSelect).setController(controller).setFrequency(SPI_FREQUENCY).build(), dcPin,
					resetPin);
		}

		public SpiCommunicationChannel(int controller, int chipSelect, int spiFrequency, DigitalOutputDevice dcPin,
				DigitalOutputDevice resetPin) {
			this(SpiDevice.builder(chipSelect).setController(controller).setFrequency(spiFrequency).build(), dcPin,
					resetPin);
		}

		public SpiCommunicationChannel(SpiDeviceInterface device, DigitalOutputDevice dcPin,
				DigitalOutputDevice resetPin) {
			this.device = device;
			this.dcPin = dcPin;
			this.resetPin = resetPin;
		}

		@Override
		public void write(byte... buffer) {
			device.write(buffer);
		}

		@Override
		public void write(byte[] txBuffer, int txOffset, int length) {
			device.write(txBuffer, txOffset, length);
		}

		@Override
		public void close() {
			Logger.trace("close()");
			device.close();
		}

		@Override
		public void reset() {
			resetPin.setOn(true);
			SleepUtil.sleepMillis(1);
			resetPin.setOn(false);
			SleepUtil.sleepMillis(10);
			resetPin.setOn(true);
		}

		@Override
		public void sendCommand(byte... commands) {
			dcPin.setOn(false);
			device.write(commands);
		}

		@Override
		public void sendData(byte... buffer) {
			dcPin.setOn(true);
			device.write(buffer);
		}

		@Override
		public void sendData(byte[] buffer, int offset, int length) {
			dcPin.setOn(true);
			device.write(buffer, offset, length);
		}
	}

	/**
	 * I2C channel. Sends the buffer data to the device in configurable chunks to
	 * adjust for the I2C speed.
	 */
	class I2cCommunicationChannel implements SsdOledCommunicationChannel {
		public static final byte DEFAULT_I2C_COMMAND = (byte) 0x80;
		public static final byte DEFAULT_I2C_DATA = (byte) 0x40;

		private final I2CDeviceInterface device;
		private final byte commandByte;
		private final byte dataByte;

		public I2cCommunicationChannel(I2CDeviceInterface device) {
			this(device, DEFAULT_I2C_COMMAND, DEFAULT_I2C_DATA);
		}

		public I2cCommunicationChannel(I2CDeviceInterface device, byte commandByte, byte dataByte) {
			this.device = device;
			this.commandByte = commandByte;
			this.dataByte = dataByte;
		}

		@Override
		public void write(byte... buffer) {
			device.writeBytes(buffer);
		}

		@Override
		public void write(byte[] buffer, int offset, int length) {
			byte[] data = new byte[length];
			System.arraycopy(buffer, offset, data, 0, length);
			device.writeBytes(data);
		}

		@Override
		public void close() {
			Logger.trace("close()");
			device.close();
		}

		@Override
		public void sendCommand(byte... commands) {
			byte[] output = new byte[2];
			output[0] = commandByte;
			for (byte command : commands) {
				output[1] = command;
				write(output);
			}
		}

		@Override
		public void sendData(byte... buffer) {
			sendData(buffer, 0, buffer.length);
		}

		@Override
		public void sendData(byte[] buffer, int offset, int length) {
			byte[] output = new byte[length + 1];
			output[0] = dataByte;
			System.arraycopy(buffer, offset, output, 1, length);
			write(output);
		}
	}
}
