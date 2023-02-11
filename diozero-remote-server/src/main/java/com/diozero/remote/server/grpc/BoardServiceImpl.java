package com.diozero.remote.server.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Server
 * Filename:     BoardServiceImpl.java
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

import java.util.Map;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.message.protobuf.Board;
import com.diozero.remote.message.protobuf.BoardServiceGrpc;
import com.diozero.remote.message.protobuf.FloatResponse;
import com.diozero.remote.message.protobuf.Gpio;
import com.diozero.remote.message.protobuf.IntegerArrayResponse;
import com.diozero.remote.message.protobuf.IntegerMessage;
import com.diozero.remote.message.protobuf.IntegerResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

public class BoardServiceImpl extends BoardServiceGrpc.BoardServiceImplBase {
	private NativeDeviceFactoryInterface deviceFactory;

	public BoardServiceImpl() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public BoardServiceImpl(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
	}

	@Override
	public void getBoardInfo(Empty request, StreamObserver<Board.BoardInfoResponse> responseObserver) {
		Logger.debug("getBoardInfo request");

		Board.BoardInfoResponse.Builder response_builder = Board.BoardInfoResponse.newBuilder().setStatus(Status.OK);

		BoardInfo board_info = deviceFactory.getBoardInfo();

		response_builder.setMake(board_info.getMake()).setModel(board_info.getModel())
				.setMemory(board_info.getMemoryKb());

		for (Map.Entry<String, Map<Integer, PinInfo>> header_entry : board_info.getHeaders().entrySet()) {
			Board.HeaderInfo.Builder header_builder = Board.HeaderInfo.newBuilder().setName(header_entry.getKey());
			header_builder.addAllGpio(header_entry.getValue().values().stream().map(DiozeroProtosConverter::convert)
					.collect(Collectors.toList()));
			response_builder.addHeader(header_builder.build());
		}

		response_builder.setBoardPwmFrequency(deviceFactory.getBoardPwmFrequency());
		response_builder.setSpiBufferSize(deviceFactory.getSpiBufferSize());
		response_builder.setOsId(board_info.getOperatingSystemId());
		response_builder.setOsVersion(board_info.getOperatingSystemVersion());

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void setBoardPwmFrequency(IntegerMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("setBoardPwmFrequency request");

		deviceFactory.setBoardPwmFrequency(request.getValue());

		Response.Builder response_builder = Response.newBuilder().setStatus(Status.OK);

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getGpioMode(Gpio.Identifier request, StreamObserver<Board.GpioModeResponse> responseObserver) {
		Logger.debug("getGpioMode request");

		DeviceMode mode = deviceFactory.getGpioMode(request.getGpio());

		Board.GpioModeResponse.Builder response_builder = Board.GpioModeResponse.newBuilder().setStatus(Status.OK)
				.setMode(DiozeroProtosConverter.convert(mode));

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getGpioValue(Gpio.Identifier request, StreamObserver<IntegerResponse> responseObserver) {
		Logger.debug("getGpioValue request");

		IntegerResponse.Builder response_builder = IntegerResponse.newBuilder().setStatus(Status.OK)
				.setData(deviceFactory.getGpioValue(request.getGpio()));

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getCpuTemperature(Empty request, StreamObserver<FloatResponse> responseObserver) {
		Logger.debug("getCpuTemperature request");

		FloatResponse.Builder response_builder = FloatResponse.newBuilder().setStatus(Status.OK)
				.setData(deviceFactory.getCpuTemperature());

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getI2CBusNumbers(Empty request, StreamObserver<IntegerArrayResponse> responseObserver) {
		Logger.debug("getI2CBusNumbers request");

		IntegerArrayResponse.Builder response_builder = IntegerArrayResponse.newBuilder().setStatus(Status.OK)
				.addAllData(deviceFactory.getI2CBusNumbers());

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getI2CFunctionalities(IntegerMessage request, StreamObserver<IntegerResponse> responseObserver) {
		Logger.debug("getI2CFunctionalities request");

		IntegerResponse.Builder response_builder = IntegerResponse.newBuilder().setStatus(Status.OK)
				.setData(deviceFactory.getI2CFunctionalities(request.getValue()));

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}
}
