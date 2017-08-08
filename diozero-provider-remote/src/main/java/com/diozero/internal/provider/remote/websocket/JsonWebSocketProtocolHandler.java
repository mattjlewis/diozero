package com.diozero.internal.provider.remote.websocket;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     JsonWebSocketProtocolHandler.java  
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.remote.BaseAsyncProtocolHandler;
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
import com.diozero.remote.websocket.MessageWrapper;
import com.diozero.remote.websocket.MessageWrapperTypes;
import com.diozero.util.RuntimeIOException;
import com.google.gson.Gson;

public class JsonWebSocketProtocolHandler extends BaseAsyncProtocolHandler implements WebSocketListener {
	private static final long TIMEOUT_MS = 1000;

	private static final Gson GSON = new Gson();

	private Serialiser serialiser;
	private Deserialiser deserialiser;
	private WebSocketClient webSocketClient;
	private Session session;

	public JsonWebSocketProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
		super(deviceFactory);

		serialiser = GSON::toJson;
		deserialiser = GSON::fromJson;
		
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
		// webSocketClient.destroy();
	}

	Response requestResponse(Object request) {
		String correlation_id = UUID.randomUUID().toString();
		Condition condition = lock.newCondition();
		conditions.put(correlation_id, condition);

		lock.lock();
		try {
			session.getRemote().sendString(serialiser.toString(
					new MessageWrapper(correlation_id, request.getClass().getSimpleName(), serialiser.toString(request))));
			condition.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}

		Response response = responses.remove(correlation_id);
		if (response == null) {
			throw new RuntimeIOException("Cannot find response message for " + correlation_id);
		}

		return response;
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response sendRequest(ProvisionDigitalOutputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputOutputDevice request) {
		return requestResponse(request);
	}

	@Override
	public GpioDigitalReadResponse sendRequest(GpioDigitalRead request) {
		return (GpioDigitalReadResponse) requestResponse(request);
	}

	@Override
	public Response sendRequest(GpioDigitalWrite request) {
		return requestResponse(request);
	}

	@Override
	public Response sendRequest(GpioEvents request) {
		return requestResponse(request);
	}

	@Override
	public Response sendRequest(GpioClose request) {
		return requestResponse(request);
	}

	@Override
	public Response sendRequest(ProvisionSpiDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response sendRequest(SpiWrite request) {
		return requestResponse(request);
	}

	@Override
	public SpiResponse sendRequest(SpiWriteAndRead request) {
		return (SpiResponse) requestResponse(request);
	}

	@Override
	public Response sendRequest(SpiClose request) {
		return requestResponse(request);
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
		// TODO Auto-generated method stub
	}

	@Override
	public void onWebSocketText(String message) {
		MessageWrapper message_wrapper = deserialiser.fromString(message, MessageWrapper.class);

		switch (message_wrapper.getType()) {
		case MessageWrapperTypes.RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), Response.class),
					message_wrapper.getCorrelationId());
			break;
		case MessageWrapperTypes.GPIO_DIGITAL_READ_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), GpioDigitalReadResponse.class),
					message_wrapper.getCorrelationId());
			break;
		case MessageWrapperTypes.SPI_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), SpiResponse.class),
					message_wrapper.getCorrelationId());
			break;

		case MessageWrapperTypes.DIGITAL_INPUT_EVENT:
			DigitalInputEvent event = deserialiser.fromString(message_wrapper.getMessage(), DigitalInputEvent.class);
			processEvent(event);
			break;

		default:
			Logger.error("Unrecognised response message type '{}'", message_wrapper.getType());
		}
	}

	@FunctionalInterface
	public static interface Serialiser {
		String toString(Object o);
	}
	
	public static interface Deserialiser {
		<T> T fromString(String source, Class<T> classOfT);
	}
}
