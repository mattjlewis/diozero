package com.diozero.remote.server.grpc;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants.DataBits;
import com.diozero.api.SerialConstants.Parity;
import com.diozero.api.SerialConstants.StopBits;
import com.diozero.internal.spi.InternalDeviceInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
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
	public void open(Serial.OpenRequest request, StreamObserver<Response> responseObserver) {
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
	public void read(Serial.ReadRequest request, StreamObserver<Serial.ReadResponse> responseObserver) {
		Logger.debug("Serial read request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Serial.ReadResponse.Builder response_builder = Serial.ReadResponse.newBuilder();

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
	public void readByte(Serial.ReadByteRequest request, StreamObserver<Serial.ReadByteResponse> responseObserver) {
		Logger.debug("Serial read byte request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Serial.ReadByteResponse.Builder response_builder = Serial.ReadByteResponse.newBuilder();

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
	public void writeByte(Serial.WriteByteRequest request, StreamObserver<Response> responseObserver) {
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
				device.writeByte((byte) request.getData());

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
	public void readBytes(Serial.ReadBytesRequest request, StreamObserver<Serial.ReadBytesResponse> responseObserver) {
		Logger.debug("Serial read bytes request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Serial.ReadBytesResponse.Builder response_builder = Serial.ReadBytesResponse.newBuilder();

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
	public void writeBytes(Serial.WriteBytesRequest request, StreamObserver<Response> responseObserver) {
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
	public void bytesAvailable(Serial.BytesAvailableRequest request,
			StreamObserver<Serial.BytesAvailableResponse> responseObserver) {
		Logger.debug("Serial bytes available request");

		String device_filename = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_filename);

		Serial.BytesAvailableResponse.Builder response_builder = Serial.BytesAvailableResponse.newBuilder();

		InternalSerialDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("Serial device not provisioned");
		} else {
			try {
				response_builder.setBytesAvailable(device.bytesAvailable());
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
	public void close(Serial.CloseRequest request, StreamObserver<Response> responseObserver) {
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
