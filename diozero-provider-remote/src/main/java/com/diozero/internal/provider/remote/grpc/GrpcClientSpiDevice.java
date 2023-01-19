package com.diozero.internal.provider.remote.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Provider
 * Filename:     GrpcClientSpiDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.message.protobuf.BytesResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.SPI;
import com.diozero.remote.message.protobuf.SPIServiceGrpc.SPIServiceBlockingStub;
import com.diozero.remote.message.protobuf.Status;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;

public class GrpcClientSpiDevice extends AbstractDevice implements InternalSpiDeviceInterface {
	private SPIServiceBlockingStub spiBlockingStub;
	private int controller;
	private int chipSelect;

	public GrpcClientSpiDevice(GrpcClientDeviceFactory deviceFactory, String key, int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) {
		super(key, deviceFactory);

		spiBlockingStub = deviceFactory.getSpiServiceStub();

		this.controller = controller;
		this.chipSelect = chipSelect;

		try {
			Response response = spiBlockingStub.open(
					SPI.Open.newBuilder().setController(controller).setChipSelect(chipSelect).setFrequency(frequency)
							.setClockMode(DiozeroProtosConverter.convert(spiClockMode)).setLsbFirst(lsbFirst).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in SPI open: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in SPI open: " + e, e);
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
	public void write(byte... txBuffer) {
		write(txBuffer, 0, txBuffer.length);
	}

	@Override
	public void write(byte[] txBuffer, int txOffset, int length) {
		try {
			byte[] data = new byte[length];
			System.arraycopy(txBuffer, txOffset, data, 0, length);
			Response response = spiBlockingStub.write(SPI.ByteArray.newBuilder().setController(controller)
					.setChipSelect(chipSelect).setTxData(ByteString.copyFrom(data)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in SPI write: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in SPI write: " + e, e);
		}
	}

	@Override
	public byte[] writeAndRead(byte... txBuffer) throws RuntimeIOException {
		try {
			BytesResponse response = spiBlockingStub.writeAndRead(SPI.ByteArray.newBuilder().setController(controller)
					.setChipSelect(chipSelect).setTxData(ByteString.copyFrom(txBuffer)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in SPI write and read: " + response.getDetail());
			}

			return response.getData().toByteArray();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in SPI write and read: " + e, e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		try {
			Response response = spiBlockingStub
					.close(SPI.Identifier.newBuilder().setController(controller).setChipSelect(chipSelect).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in SPI close: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in SPI close: " + e, e);
		}
	}
}
