package com.diozero.internal.provider.remote.websocket;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     ProtobufWebSocketProtocolHandler.java
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

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.remote.ProtobufBaseAsyncProtocolHandler;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.remote.message.DiozeroProtosConverter;
import com.diozero.remote.message.GetBoardInfoRequest;
import com.diozero.remote.message.GetBoardInfoResponse;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioGetPwmFrequency;
import com.diozero.remote.message.GpioGetPwmFrequencyResponse;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.GpioSetPwmFrequency;
import com.diozero.remote.message.I2CBlockProcessCall;
import com.diozero.remote.message.I2CBooleanResponse;
import com.diozero.remote.message.I2CByteResponse;
import com.diozero.remote.message.I2CBytesResponse;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CProbe;
import com.diozero.remote.message.I2CProcessCall;
import com.diozero.remote.message.I2CReadBlockData;
import com.diozero.remote.message.I2CReadBlockDataResponse;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadBytes;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadWordData;
import com.diozero.remote.message.I2CWordResponse;
import com.diozero.remote.message.I2CWriteBlockData;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteBytes;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.I2CWriteQuick;
import com.diozero.remote.message.I2CWriteWordData;
import com.diozero.remote.message.ProvisionAnalogInputDevice;
import com.diozero.remote.message.ProvisionAnalogOutputDevice;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionPwmOutputDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SerialBytesAvailable;
import com.diozero.remote.message.SerialBytesAvailableResponse;
import com.diozero.remote.message.SerialClose;
import com.diozero.remote.message.SerialOpen;
import com.diozero.remote.message.SerialRead;
import com.diozero.remote.message.SerialReadByte;
import com.diozero.remote.message.SerialReadByteResponse;
import com.diozero.remote.message.SerialReadBytes;
import com.diozero.remote.message.SerialReadBytesResponse;
import com.diozero.remote.message.SerialReadResponse;
import com.diozero.remote.message.SerialWriteByte;
import com.diozero.remote.message.SerialWriteBytes;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.remote.message.protobuf.DiozeroProtos;
import com.diozero.remote.websocket.MessageWrapperTypes;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;

public class ProtobufWebSocketProtocolHandler extends ProtobufBaseAsyncProtocolHandler implements WebSocketListener {
	private static final String URL = "/diozero";

	private WebSocketClient webSocketClient;
	private Session session;

	public ProtobufWebSocketProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
		super(deviceFactory);

		webSocketClient = new WebSocketClient();
	}

	@Override
	public void start() {
		try {
			webSocketClient.start();

			URI uri = new URI("ws://localhost:8080/diozero");
			Logger.debug("Connecting to: {}...", uri);
			session = webSocketClient.connect(this, uri, new ClientUpgradeRequest()).get();
			Logger.debug("Connected to: {}", uri);
		} catch (Exception e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void close() {
		session.close();
		// webSocketClient.destroy();
	}

	@Override
	protected void sendMessage(String url, GeneratedMessageV3 message) throws IOException {
		DiozeroProtos.MessageWrapper message_wrapper = DiozeroProtos.MessageWrapper.newBuilder()
				.setType(message.getClass().getSimpleName()).setMessage(ByteString.copyFrom(message.toByteArray()))
				.build();

		session.getRemote().sendBytes(ByteBuffer.wrap(message_wrapper.toByteArray()));
	}

	@Override
	public GetBoardInfoResponse request(GetBoardInfoRequest request) {
		return (GetBoardInfoResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		return (GpioDigitalReadResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		return (GpioPwmReadResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(GpioPwmWrite request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioGetPwmFrequencyResponse request(GpioGetPwmFrequency request) {
		return (GpioGetPwmFrequencyResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(GpioSetPwmFrequency request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		return (GpioAnalogReadResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioEvents request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioClose request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(I2COpen request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CBooleanResponse request(I2CProbe request) {
		return (I2CBooleanResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteQuick request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CByteResponse request(I2CReadByte request) {
		return (I2CByteResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteByte request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CBytesResponse request(I2CReadBytes request) {
		return (I2CBytesResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteBytes request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CByteResponse request(I2CReadByteData request) {
		return (I2CByteResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteByteData request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CWordResponse request(I2CReadWordData request) {
		return (I2CWordResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteWordData request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CWordResponse request(I2CProcessCall request) {
		return (I2CWordResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public I2CReadBlockDataResponse request(I2CReadBlockData request) {
		return (I2CReadBlockDataResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteBlockData request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CBytesResponse request(I2CBlockProcessCall request) {
		return (I2CBytesResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public I2CBytesResponse request(I2CReadI2CBlockData request) {
		return (I2CBytesResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(I2CClose request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SpiOpen request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SpiWrite request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		return (SpiResponse) requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SpiClose request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SerialOpen request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public SerialReadResponse request(SerialRead request) {
		return (SerialReadResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public SerialReadByteResponse request(SerialReadByte request) {
		return (SerialReadByteResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(SerialWriteByte request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public SerialReadBytesResponse request(SerialReadBytes request) {
		return (SerialReadBytesResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(SerialWriteBytes request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public SerialBytesAvailableResponse request(SerialBytesAvailable request) {
		return (SerialBytesAvailableResponse) requestResponse(URL, DiozeroProtosConverter.convert(request),
				request.getCorrelationId());
	}

	@Override
	public Response request(SerialClose request) {
		return requestResponse(URL, DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onWebSocketConnect(Session newSession) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		ByteBuffer buffer = ByteBuffer.wrap(payload, offset, len);

		try {
			DiozeroProtos.MessageWrapper message_wrapper = DiozeroProtos.MessageWrapper.parseFrom(buffer);

			switch (message_wrapper.getType()) {
			case MessageWrapperTypes.RESPONSE:
				DiozeroProtos.Response response = DiozeroProtos.Response
						.parseFrom(message_wrapper.getMessage().toByteArray());
				processResponse(DiozeroProtosConverter.convert(response));
				break;
			case MessageWrapperTypes.GPIO_DIGITAL_READ_RESPONSE:
				DiozeroProtos.Gpio.DigitalReadResponse digital_read_response = DiozeroProtos.Gpio.DigitalReadResponse
						.parseFrom(message_wrapper.getMessage().toByteArray());
				processResponse(DiozeroProtosConverter.convert(digital_read_response));
				break;
			case MessageWrapperTypes.SPI_RESPONSE:
				DiozeroProtos.Spi.SpiResponse spi_response = DiozeroProtos.Spi.SpiResponse
						.parseFrom(message_wrapper.getMessage().toByteArray());
				processResponse(DiozeroProtosConverter.convert(spi_response));
				break;

			case MessageWrapperTypes.DIGITAL_INPUT_EVENT:
				DigitalInputEvent event = DiozeroProtosConverter
						.convert(DiozeroProtos.Gpio.Notification.parseFrom(message_wrapper.getMessage().toByteArray()));
				processEvent(event);
				break;

			default:
				Logger.error("Unrecognised response message type '{}'", message_wrapper.getType());
			}
		} catch (InvalidProtocolBufferException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	@Override
	public void onWebSocketText(String message) {
	}
}
