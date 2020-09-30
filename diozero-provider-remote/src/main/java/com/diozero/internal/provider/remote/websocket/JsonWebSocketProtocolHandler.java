package com.diozero.internal.provider.remote.websocket;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     JsonWebSocketProtocolHandler.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.remote.BaseAsyncProtocolHandler;
import com.diozero.remote.message.GetBoardInfo;
import com.diozero.remote.message.GetBoardInfoResponse;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CRead;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadByteResponse;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadResponse;
import com.diozero.remote.message.I2CWrite;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.ProvisionAnalogInputDevice;
import com.diozero.remote.message.ProvisionAnalogOutputDevice;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionPwmOutputDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.remote.server.websocket.MessageWrapper;
import com.diozero.remote.server.websocket.MessageWrapperTypes;
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
					new MessageWrapper(request.getClass().getSimpleName(), serialiser.toString(request))));
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
	public GetBoardInfoResponse request(GetBoardInfo request) {
		return (GetBoardInfoResponse) requestResponse(request);
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		return requestResponse(request);
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		return requestResponse(request);
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		return (GpioDigitalReadResponse) requestResponse(request);
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		return requestResponse(request);
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		return (GpioPwmReadResponse) requestResponse(request);
	}

	@Override
	public Response request(GpioPwmWrite request) {
		return requestResponse(request);
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		return (GpioAnalogReadResponse) requestResponse(request);
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		return requestResponse(request);
	}

	@Override
	public Response request(GpioEvents request) {
		return requestResponse(request);
	}

	@Override
	public Response request(GpioClose request) {
		return requestResponse(request);
	}

	@Override
	public Response request(I2COpen request) {
		return requestResponse(request);
	}

	@Override
	public I2CReadByteResponse request(I2CReadByte request) {
		return (I2CReadByteResponse) requestResponse(request);
	}

	@Override
	public Response request(I2CWriteByte request) {
		return requestResponse(request);
	}

	@Override
	public I2CReadResponse request(I2CRead request) {
		return (I2CReadResponse) requestResponse(request);
	}

	@Override
	public Response request(I2CWrite request) {
		return requestResponse(request);
	}

	@Override
	public I2CReadByteResponse request(I2CReadByteData request) {
		return (I2CReadByteResponse) requestResponse(request);
	}

	@Override
	public Response request(I2CWriteByteData request) {
		return requestResponse(request);
	}

	@Override
	public I2CReadResponse request(I2CReadI2CBlockData request) {
		return (I2CReadResponse) requestResponse(request);
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		return requestResponse(request);
	}
	
	@Override
	public Response request(I2CClose request) {
		return requestResponse(request);
	}

	@Override
	public Response request(SpiOpen request) {
		return requestResponse(request);
	}
	
	@Override
	public Response request(SpiWrite request) {
		return requestResponse(request);
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		return (SpiResponse) requestResponse(request);
	}

	@Override
	public Response request(SpiClose request) {
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
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), Response.class));
			break;

		case MessageWrapperTypes.GPIO_DIGITAL_READ_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), GpioDigitalReadResponse.class));
			break;
		case MessageWrapperTypes.GPIO_PWM_READ_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), GpioPwmReadResponse.class));
			break;
		case MessageWrapperTypes.GPIO_ANALOG_READ_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), GpioAnalogReadResponse.class));
			break;
		case MessageWrapperTypes.DIGITAL_INPUT_EVENT:
			DigitalInputEvent event = deserialiser.fromString(message_wrapper.getMessage(), DigitalInputEvent.class);
			processEvent(event);
			break;

		case MessageWrapperTypes.I2C_READ_BYTE_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), I2CReadByteResponse.class));
			break;
		case MessageWrapperTypes.I2C_READ_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), I2CReadResponse.class));
			break;

		case MessageWrapperTypes.SPI_RESPONSE:
			processResponse(deserialiser.fromString(message_wrapper.getMessage(), SpiResponse.class));
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
