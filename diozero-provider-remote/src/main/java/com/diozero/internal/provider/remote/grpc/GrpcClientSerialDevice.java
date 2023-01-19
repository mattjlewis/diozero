package com.diozero.internal.provider.remote.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Provider
 * Filename:     GrpcClientSerialDevice.java
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
import com.diozero.api.SerialDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.remote.message.protobuf.ByteResponse;
import com.diozero.remote.message.protobuf.BytesResponse;
import com.diozero.remote.message.protobuf.IntegerResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.Serial;
import com.diozero.remote.message.protobuf.SerialServiceGrpc.SerialServiceBlockingStub;
import com.diozero.remote.message.protobuf.Status;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;

public class GrpcClientSerialDevice extends AbstractDevice implements InternalSerialDeviceInterface {
	private SerialServiceBlockingStub serialBlockingStub;
	private String deviceFile;

	public GrpcClientSerialDevice(GrpcClientDeviceFactory deviceFactory, String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) {
		super(key, deviceFactory);

		serialBlockingStub = deviceFactory.getSerialServiceStub();
		this.deviceFile = deviceFile;

		try {
			Response response = serialBlockingStub.open(
					Serial.Open.newBuilder().setDeviceFile(deviceFile).setBaud(baud).setDataBits(dataBits.ordinal())
							.setStopBits(stopBits.ordinal()).setParity(parity.ordinal()).setReadBlocking(readBlocking)
							.setMinReadChars(minReadChars).setReadTimeoutMillis(readTimeoutMillis).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial open: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial open: " + e, e);
		}
	}

	@Override
	public int read() {
		try {
			IntegerResponse response = serialBlockingStub
					.read(Serial.Identifier.newBuilder().setDeviceFile(deviceFile).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial read: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial read: " + e, e);
		}
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		try {
			ByteResponse response = serialBlockingStub
					.readByte(Serial.Identifier.newBuilder().setDeviceFile(deviceFile).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial read byte: " + response.getDetail());
			}

			return (byte) response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial read byte: " + e, e);
		}
	}

	@Override
	public void writeByte(byte bVal) {
		try {
			Response response = serialBlockingStub
					.writeByte(Serial.ByteMessage.newBuilder().setDeviceFile(deviceFile).setValue(bVal).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial write byte: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial write byte: " + e, e);
		}
	}

	@Override
	public int read(byte[] buffer) {
		try {
			BytesResponse response = serialBlockingStub
					.readBytes(Serial.NumBytes.newBuilder().setDeviceFile(deviceFile).setLength(buffer.length).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial read bytes: " + response.getDetail());
			}

			byte[] result = response.getData().toByteArray();
			System.arraycopy(result, 0, buffer, 0, result.length);

			return result.length;
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial read bytes: " + e, e);
		}
	}

	@Override
	public void write(byte... data) {
		try {
			Response response = serialBlockingStub.writeBytes(
					Serial.ByteArray.newBuilder().setDeviceFile(deviceFile).setData(ByteString.copyFrom(data)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial write bytes: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial write bytes: " + e, e);
		}
	}

	@Override
	public int bytesAvailable() {
		try {
			IntegerResponse response = serialBlockingStub
					.bytesAvailable(Serial.Identifier.newBuilder().setDeviceFile(deviceFile).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial bytes available: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial bytes available: " + e, e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		try {
			Response response = serialBlockingStub
					.close(Serial.Identifier.newBuilder().setDeviceFile(deviceFile).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Serial close: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Serial close: " + e, e);
		}
	}
}
