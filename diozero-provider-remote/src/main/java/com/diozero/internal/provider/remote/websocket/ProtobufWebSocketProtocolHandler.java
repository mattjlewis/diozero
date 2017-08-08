package com.diozero.internal.provider.remote.websocket;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     ProtobufWebSocketProtocolHandler.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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
import java.util.UUID;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.remote.ProtobufBaseAsyncProtocolHandler;
import com.diozero.remote.message.DiozeroProtos;
import com.diozero.remote.message.DiozeroProtosConverter;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionSpiDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.remote.websocket.MessageWrapperTypes;
import com.diozero.util.RuntimeIOException;
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
		//webSocketClient.destroy();
	}

	@Override
	protected void sendMessage(String url, GeneratedMessageV3 message) throws IOException {
		DiozeroProtos.MessageWrapper message_wrapper = DiozeroProtos.MessageWrapper.newBuilder()
				.setType(message.getClass().getSimpleName()).setMessage(ByteString.copyFrom(message.toByteArray()))
				.build();

		session.getRemote().sendBytes(ByteBuffer.wrap(message_wrapper.toByteArray()));
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(ProvisionDigitalOutputDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputOutputDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public GpioDigitalReadResponse sendRequest(GpioDigitalRead request) {
		String correlation_id = UUID.randomUUID().toString();
		return (GpioDigitalReadResponse) requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id),
				correlation_id);
	}

	@Override
	public Response sendRequest(GpioDigitalWrite request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(GpioEvents request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(GpioClose request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(ProvisionSpiDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(SpiWrite request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public SpiResponse sendRequest(SpiWriteAndRead request) {
		String correlation_id = UUID.randomUUID().toString();
		return (SpiResponse) requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id),
				correlation_id);
	}

	@Override
	public Response sendRequest(SpiClose request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(URL, DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
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
				processResponse(DiozeroProtosConverter.convert(response), response.getCorrelationId());
				break;
			case MessageWrapperTypes.GPIO_DIGITAL_READ_RESPONSE:
				DiozeroProtos.Gpio.DigitalReadResponse digital_read_response = DiozeroProtos.Gpio.DigitalReadResponse
						.parseFrom(message_wrapper.getMessage().toByteArray());
				processResponse(DiozeroProtosConverter.convert(digital_read_response),
						digital_read_response.getCorrelationId());
				break;
			case MessageWrapperTypes.SPI_RESPONSE:
				DiozeroProtos.Spi.SpiResponse spi_response = DiozeroProtos.Spi.SpiResponse
						.parseFrom(message_wrapper.getMessage().toByteArray());
				processResponse(DiozeroProtosConverter.convert(spi_response), spi_response.getCorrelationId());
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
