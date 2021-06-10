package com.diozero.remote.server.grpc;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.InternalDeviceInterface;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.message.protobuf.BytesResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.SPI;
import com.diozero.remote.message.protobuf.SPIServiceGrpc;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.sbc.DeviceFactoryHelper;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

public class SpiServiceImpl extends SPIServiceGrpc.SPIServiceImplBase {
	private NativeDeviceFactoryInterface deviceFactory;

	public SpiServiceImpl() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public SpiServiceImpl(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
	}

	@Override
	public void open(SPI.Open request, StreamObserver<Response> responseObserver) {
		Logger.debug("SPI open request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		Response.Builder response_builder = Response.newBuilder();

		InternalDeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("SPI device already provisioned");
		} else {
			try {
				device = deviceFactory.provisionSpiDevice(controller, chip_select, request.getFrequency(),
						DiozeroProtosConverter.convert(request.getClockMode()), request.getLsbFirst());

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
	public void write(SPI.ByteArray request, StreamObserver<Response> responseObserver) {
		Logger.debug("SPI write request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		Response.Builder response_builder = Response.newBuilder();

		InternalSpiDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("SPI device not provisioned");
		} else {
			try {
				byte[] data = request.getTxData().toByteArray();
				device.write(data);

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
	public void writeAndRead(SPI.ByteArray request, StreamObserver<BytesResponse> responseObserver) {
		Logger.debug("SPI write and read request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		BytesResponse.Builder response_builder = BytesResponse.newBuilder();

		InternalSpiDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("SPI device not provisioned");
		} else {
			try {
				byte[] data = request.getTxData().toByteArray();
				device.writeAndRead(data);

				response_builder.setData(ByteString.copyFrom(data));
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
