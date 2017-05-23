package com.diozero.internal.provider.jdkdio10;

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
	
	public JdkDeviceIoSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int chipSelect, int frequency, SpiClockMode mode, boolean lsbFirst) throws RuntimeIOException {
		super(key, deviceFactory);
		
		int cs_active = SPIDeviceConfig.CS_ACTIVE_LOW;
		//int clock_frequency = SPIDeviceConfig.DEFAULT;
		//int clock_frequency = RPiConstants.SPI_DEFAULT_CLOCK;
		int word_length = SPIConstants.DEFAULT_WORD_LENGTH;
		int bit_ordering = lsbFirst ? Device.LITTLE_ENDIAN : Device.BIG_ENDIAN;
		deviceConfig = new SPIDeviceConfig(controller, chipSelect, cs_active,
				frequency, mode.getMode(), word_length, bit_ordering);
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
	public void write(ByteBuffer src) {
		if (! device.isOpen()) {
			throw new IllegalStateException("SPI Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		try {
			device.write(src);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer src) throws RuntimeIOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("SPI Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		ByteBuffer dest = ByteBuffer.allocateDirect(src.capacity());
		try {
			device.writeAndRead(src, dest);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return dest;
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
