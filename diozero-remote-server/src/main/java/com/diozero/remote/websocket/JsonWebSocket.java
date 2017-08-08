package com.diozero.remote.websocket;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     JsonWebSocket.java  
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

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.webSocket;

import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.remote.BaseRemoteServer;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionSpiDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.google.gson.Gson;

@SuppressWarnings("static-method")
@WebSocket
public class JsonWebSocket extends BaseRemoteServer {
	private static final Gson GSON = new Gson();
	private static Queue<Session> sessions = new ConcurrentLinkedQueue<>();

	public static void main(String[] args) {
		port(8080);
		webSocket("/diozero", JsonWebSocket.class);
		init(); // Needed if you don't define any HTTP routes after your WebSocket routes
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		Logger.debug("New connection from: {}", session.getRemoteAddress().getHostName());
		sessions.add(session);
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		Logger.debug("Connection closed from: {}", session.getRemoteAddress().getHostName());
		sessions.remove(session);
	}

	@OnWebSocketMessage
	public void onMessage(Session session, String message) {
		Logger.debug("Got message: {}", message);
		MessageWrapper wrapper = GSON.fromJson(message, MessageWrapper.class);
		Response response = null;
		switch (wrapper.getType()) {
		case MessageWrapperTypes.PROVISION_DIGITAL_INPUT_DEVICE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), ProvisionDigitalInputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_DIGITAL_OUTPUT_DEVICE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), ProvisionDigitalOutputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_DIGITAL_INPUT_OUTPUT_DEVICE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), ProvisionDigitalInputOutputDevice.class));
			break;
		case MessageWrapperTypes.GPIO_DIGITAL_READ:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), GpioDigitalRead.class));
			break;
		case MessageWrapperTypes.GPIO_DIGITAL_WRITE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), GpioDigitalWrite.class));
			break;
		case MessageWrapperTypes.GPIO_EVENTS:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), GpioEvents.class));
			break;
		case MessageWrapperTypes.GPIO_CLOSE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), GpioClose.class));
			break;

		case MessageWrapperTypes.PROVISION_SPI_DEVICE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), ProvisionSpiDevice.class));
			break;
		case MessageWrapperTypes.SPI_WRITE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), SpiWrite.class));
			break;
		case MessageWrapperTypes.SPI_WRITE_AND_READ:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), SpiWriteAndRead.class));
			break;
		case MessageWrapperTypes.SPI_CLOSE:
			response = processRequest(GSON.fromJson(wrapper.getMessage(), SpiClose.class));
			break;
		default:
			Logger.warn("Unhandled message type '{}'", wrapper.getType());
		}

		if (response != null) {
			sendMessage(session, response);
		}
	}

	@Override
	public void valueChanged(DigitalInputEvent event) {
		MessageWrapper message = new MessageWrapper(event.getClass().getSimpleName(), GSON.toJson(event));
		sessions.forEach(session -> {
			try {
				session.getRemote().sendString(GSON.toJson(message));
			} catch (IOException e) {
				Logger.error(e, "Error: {}", e);
				// TODO Cleanup this session?
			}
		});
	}

	private void sendMessage(Session session, Object o) {
		MessageWrapper wrapper = new MessageWrapper(UUID.randomUUID().toString(), o.getClass().getSimpleName(),
				GSON.toJson(o));
		try {
			session.getRemote().sendString(GSON.toJson(wrapper));
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
			// TODO Cleanup this session?
		}
	}
}
