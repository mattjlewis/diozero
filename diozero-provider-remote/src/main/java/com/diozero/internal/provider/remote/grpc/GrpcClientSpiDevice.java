package com.diozero.internal.provider.remote.grpc;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import com.diozero.remote.DiozeroProtosConverter;
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
			Response response = spiBlockingStub.open(SPI.OpenRequest.newBuilder().setController(controller)
					.setChipSelect(chipSelect).setFrequency(frequency)
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
			Response response = spiBlockingStub.write(SPI.WriteRequest.newBuilder().setController(controller)
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
			SPI.SpiResponse response = spiBlockingStub
					.writeAndRead(SPI.WriteAndReadRequest.newBuilder().setController(controller)
							.setChipSelect(chipSelect).setTxData(ByteString.copyFrom(txBuffer)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in SPI write and read: " + response.getDetail());
			}

			return response.getRxData().toByteArray();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in SPI write and read: " + e, e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		try {
			Response response = spiBlockingStub
					.close(SPI.CloseRequest.newBuilder().setController(controller).setChipSelect(chipSelect).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in SPI close: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in SPI close: " + e, e);
		}
	}
}
