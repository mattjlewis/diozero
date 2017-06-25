package com.diozero.internal.provider.jdkdio11;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
 * %%
 * Copyright (C) 2016 diozero
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
import java.nio.ByteBuffer;

import org.pmw.tinylog.Logger;

import com.diozero.api.SPIConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.RuntimeIOException;

import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.spibus.SPIDevice;
import jdk.dio.spibus.SPIDeviceConfig;

public class JdkDeviceIoSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private SPIDeviceConfig deviceConfig;
	private SPIDevice device;
	
	public JdkDeviceIoSpiDevice(String key, DeviceFactoryInterface deviceFactory,
			int controller, int chipSelect, int frequency, SpiClockMode mode, boolean lsbFirst) throws RuntimeIOException {
		super(key, deviceFactory);
		
		int cs_active = SPIDeviceConfig.CS_ACTIVE_LOW;
		//int clock_frequency = SPIDeviceConfig.DEFAULT;
		//int clock_frequency = RPiConstants.SPI_DEFAULT_CLOCK;
		int word_length = SPIConstants.DEFAULT_WORD_LENGTH;
		int bit_ordering = lsbFirst ? Device.LITTLE_ENDIAN : Device.BIG_ENDIAN;
		deviceConfig = new SPIDeviceConfig.Builder().setControllerNumber(controller).setAddress(chipSelect)
				.setCSActiveLevel(cs_active).setClockFrequency(frequency).setClockMode(mode.getMode())
				.setWordLength(word_length).setBitOrdering(bit_ordering).build();
		try {
			device = DeviceManager.open(deviceConfig);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		if (device.isOpen()) {
			try {
				device.close();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
	
	@Override
	public void write(byte[] txBuffer) {
		if (! device.isOpen()) {
			throw new IllegalStateException("SPI Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		try {
			device.write(ByteBuffer.wrap(txBuffer));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public void write(byte[] txBuffer, int offset, int length) {
		if (! device.isOpen()) {
			throw new IllegalStateException("SPI Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		try {
			device.write(ByteBuffer.wrap(txBuffer, offset, length));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public byte[] writeAndRead(byte[] txBuffer) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("SPI Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		ByteBuffer rx_buffer = ByteBuffer.allocateDirect(txBuffer.length);
		try {
			device.writeAndRead(ByteBuffer.wrap(txBuffer), rx_buffer);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
		byte[] rx = new byte[rx_buffer.remaining()];
		rx_buffer.get(rx, 0, rx.length);
		
		return rx;
	}

	@Override
	public int getController() {
		return deviceConfig.getControllerNumber();
	}

	@Override
	public int getChipSelect() {
		return deviceConfig.getAddress();
	}
}
