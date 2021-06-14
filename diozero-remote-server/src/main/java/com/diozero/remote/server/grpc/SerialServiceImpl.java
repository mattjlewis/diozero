package com.diozero.remote.server.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Server
 * Filename:     SerialServiceImpl.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants.DataBits;
import com.diozero.api.SerialConstants.Parity;
import com.diozero.api.SerialConstants.StopBits;
import com.diozero.internal.spi.InternalDeviceInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.remote.message.protobuf.ByteResponse;
import com.diozero.remote.message.protobuf.BytesResponse;
import com.diozero.remote.message.protobuf.IntegerResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.Serial;
import com.diozero.remote.message.protobuf.SerialServiceGrpc;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.sbc.DeviceFactoryHelper;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

public class SerialServiceImpl extends SerialServiceGrpc.SerialServiceImplBase {
	private NativeDeviceFactoryInterface deviceFactory;

	public SerialServiceImpl() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public SerialServiceImpl(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
	}

	@Override
	public void open(Serial.Open request, StreamObserver<Response> responseObserver) {
		Logger.debug("Serial open request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Response.Builder response_builder = Response.newBuilder();

		InternalDeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device already provisioned");
		} else {
			try {
				device = deviceFactory.provisionSerialDevice(device_filename, request.getBaud(),
						DataBits.values()[request.getDataBits()], StopBits.values()[request.getStopBits()],
						Parity.values()[request.getParity()], request.getReadBlocking(), request.getMinReadChars(),
						request.getReadTimeoutMillis());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void read(Serial.Identifier request, StreamObserver<IntegerResponse> responseObserver) {
		Logger.debug("Serial read request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		IntegerResponse.Builder response_builder = IntegerResponse.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				response_builder.setData(device.read());
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readByte(Serial.Identifier request, StreamObserver<ByteResponse> responseObserver) {
		Logger.debug("Serial read byte request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		ByteResponse.Builder response_builder = ByteResponse.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				response_builder.setData(device.readByte());
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeByte(Serial.ByteMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("Serial write byte request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Response.Builder response_builder = Response.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				device.writeByte((byte) request.getValue());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readBytes(Serial.NumBytes request, StreamObserver<BytesResponse> responseObserver) {
		Logger.debug("Serial read bytes request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		BytesResponse.Builder response_builder = BytesResponse.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				byte[] buffer = new byte[request.getLength()];
				// TODO Note the number of bytes actually read is ignored
				device.read(buffer);

				response_builder.setData(ByteString.copyFrom(buffer));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeBytes(Serial.ByteArray request, StreamObserver<Response> responseObserver) {
		Logger.debug("Serial write bytes request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Response.Builder response_builder = Response.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				device.write(request.getData().toByteArray());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void bytesAvailable(Serial.Identifier request, StreamObserver<IntegerResponse> responseObserver) {
		Logger.debug("Serial bytes available request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		IntegerResponse.Builder response_builder = IntegerResponse.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				response_builder.setData(device.bytesAvailable());
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void close(Serial.Identifier request, StreamObserver<Response> responseObserver) {
		Logger.debug("Serial close request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Response.Builder response_builder = Response.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				device.close();

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}
}
