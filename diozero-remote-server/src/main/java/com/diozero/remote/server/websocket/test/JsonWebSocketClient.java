package com.diozero.remote.server.websocket.test;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     JsonWebSocketClient.java  
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

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.pmw.tinylog.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.server.websocket.MessageWrapper;
import com.diozero.util.Hex;
import com.google.gson.Gson;

public class JsonWebSocketClient implements WebSocketListener {
	private static final Gson GSON = new Gson();

	public static void main(String[] args) {
		String correlation_id = UUID.randomUUID().toString();

		WebSocketClient client = new WebSocketClient();
		JsonWebSocketClient socket = new JsonWebSocketClient();

		try {
			client.start();

			URI uri = new URI("ws://localhost:8080/diozero");
			Logger.debug("Connecting to: {}...", uri);
			Future<Session> future = client.connect(socket, uri, new ClientUpgradeRequest());
			Session session = future.get();
			Logger.debug("Connected to: {}", uri);

			SpiOpen spi_request = new SpiOpen(1, 2, 8_000_000, SpiClockMode.MODE_1, false,
					correlation_id);
			MessageWrapper message = new MessageWrapper(SpiOpen.class.getSimpleName(),
					GSON.toJson(spi_request));
			session.getRemote().sendString(GSON.toJson(message));
		} catch (Exception e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	@Override
	public void onWebSocketConnect(Session session) {
		Logger.debug("Connected");
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		Logger.debug("Closed");
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		Logger.error(cause, "Connection error: {}", cause);
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		Logger.debug("Binary data: 0x{}", Hex.encodeHexString(payload));
	}

	@Override
	public void onWebSocketText(String message) {
		Logger.debug("Text data: {}", message);
	}
}
