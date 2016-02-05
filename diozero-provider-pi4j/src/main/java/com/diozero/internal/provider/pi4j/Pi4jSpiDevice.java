package com.diozero.internal.provider.pi4j;

/*
 * #%L
 * Device I/O Zero - pi4j provider
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.SpiDeviceInterface;
import com.pi4j.io.spi.*;

public class Pi4jSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final Logger logger = LogManager.getLogger(Pi4jSpiDevice.class);

	private final SpiDevice spiDevice;
	private int controller;
	private int chipSelect;
	
	public Pi4jSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int chipSelect, int speed, SpiClockMode mode) throws IOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
		spiDevice = SpiFactory.getInstance(SpiChannel.getByNumber(chipSelect), speed, SpiMode.getByNumber(mode.getMode()));
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		// No way to close a Pi4J SPI Device?!
		//spiDevice.close();
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer out) throws IOException {
		return spiDevice.write(out);
	}

	@Override
	public int getController() {
		return controller;
	}

	@Override
	public int getChipSelect() {
		return chipSelect;
	}
}
