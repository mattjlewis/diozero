package com.diozero.devices.oled;

import org.tinylog.Logger;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SsdOledCommunicationChannel.java  
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

import com.diozero.api.I2CDevice;
import com.diozero.api.SpiDevice;

public interface SsdOledCommunicationChannel extends AutoCloseable {
	void write(byte... data);
	void write(byte[] buffer, int offset, int length);
	@Override
	void close();
	
	public static class SpiCommunicationChannel implements SsdOledCommunicationChannel {
		private SpiDevice device;
		
		public SpiCommunicationChannel(int chipSelect, int controller, int spiFrequency) {
			device = SpiDevice.builder(chipSelect).setController(controller).setFrequency(spiFrequency).build();
		}

		@Override
		public void write(byte... data) {
			device.write(data);
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
	}
	
	public static class I2cCommunicationChannel implements SsdOledCommunicationChannel {
		private I2CDevice device;

		@Override
		public void write(byte... commands) {
			// TODO Check I2C transaction size limit
			device.writeBytes(commands);
		}

		@Override
		public void write(byte[] buffer, int offset, int length) {
			// TODO Check I2C transaction size limit
			byte[] data = new byte[length];
			System.arraycopy(buffer, offset, data, 0, length);
			device.writeBytes(data);
		}

		@Override
		public void close() {
			Logger.trace("close()");
			device.close();
		}
	}
}
