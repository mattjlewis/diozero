package com.diozero.internal.provider.remote.grpc;

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
