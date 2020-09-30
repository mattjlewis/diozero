package com.diozero.internal.provider.remote.devicefactory;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Provider
 * Filename:     RemoteSpiDevice.java  
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

import java.util.UUID;

import org.tinylog.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.RuntimeIOException;

public class RemoteSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private RemoteDeviceFactory deviceFactory;
	private int controller;
	private int chipSelect;

	public RemoteSpiDevice(RemoteDeviceFactory deviceFactory, String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		this.controller = controller;
		this.chipSelect = chipSelect;

		SpiOpen request = new SpiOpen(controller, chipSelect, frequency, spiClockMode, lsbFirst,
				UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error: " + response.getDetail());
		}
	}

	@Override
	public int getController() {
		return controller;
	}

	@Override
	public int getChipSelect() {
		return chipSelect;
	}

	@Override
	public void write(byte[] txBuffer) {
		write(txBuffer, 0, txBuffer.length);
	}

	@Override
	public void write(byte[] txBuffer, int txOffset, int length) {
		SpiWrite request = new SpiWrite(controller, chipSelect, txBuffer, txOffset, length,
				UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in SPI write: " + response.getDetail());
		}
	}

	@Override
	public byte[] writeAndRead(byte[] txBuffer) throws RuntimeIOException {
		SpiWriteAndRead request = new SpiWriteAndRead(controller, chipSelect, txBuffer, UUID.randomUUID().toString());

		SpiResponse response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in SPI writeAndRead: " + response.getDetail());
		}

		return response.getRxData();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		SpiClose request = new SpiClose(controller, chipSelect, UUID.randomUUID().toString());

		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			Logger.error("Error closing device: " + response.getDetail());
		}
	}
}
