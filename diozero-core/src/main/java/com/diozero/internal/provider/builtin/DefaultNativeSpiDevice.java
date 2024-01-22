package com.diozero.internal.provider.builtin;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DefaultNativeSpiDevice.java
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

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.builtin.spi.NativeSpiDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalSpiDeviceInterface;

public class DefaultNativeSpiDevice extends AbstractDevice implements InternalSpiDeviceInterface {
	private NativeSpiDevice device;

	public DefaultNativeSpiDevice(DeviceFactoryInterface deviceFactory, String key, int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) {
		super(key, deviceFactory);

		device = new NativeSpiDevice(controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		device.close();
	}

	@Override
	public int getController() {
		return device.getController();
	}

	@Override
	public int getChipSelect() {
		return device.getChipSelect();
	}

	@Override
	public void write(byte... txBuffer) {
		device.write(txBuffer, 0);
	}

	@Override
	public void write(byte[] txBuffer, int txOffset, int length) {
		device.write(txBuffer, txOffset, length, 0);
	}

	@Override
	public byte[] writeAndRead(byte... txBuffer) throws RuntimeIOException {
		return device.writeAndRead(txBuffer, 0);
	}
}
