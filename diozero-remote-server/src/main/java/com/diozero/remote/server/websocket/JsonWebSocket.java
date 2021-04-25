package com.diozero.remote.server.websocket;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     JsonWebSocket.java
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

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.webSocket;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.I2CBlockProcessCall;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CProbe;
import com.diozero.remote.message.I2CProcessCall;
import com.diozero.remote.message.I2CReadBlockData;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadBytes;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadWordData;
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
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.remote.server.BaseRemoteServer;
import com.diozero.remote.websocket.MessageWrapper;
import com.diozero.remote.websocket.MessageWrapperTypes;
import com.google.gson.Gson;

@SuppressWarnings("static-method")
@WebSocket
public class JsonWebSocket extends BaseRemoteServer {
	private static final Gson GSON = new Gson();
	private static Queue<Session> sessions = new ConcurrentLinkedQueue<>();

	public static void main(String[] args) {
		port(8080);
		webSocket("/diozero", JsonWebSocket.class);
	}
	
	@Override
	public void start() {
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
		// GPIO
		case MessageWrapperTypes.PROVISION_DIGITAL_INPUT_DEVICE:
			response = request(GSON.fromJson(wrapper.getMessage(), ProvisionDigitalInputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_DIGITAL_OUTPUT_DEVICE:
			response = request(GSON.fromJson(wrapper.getMessage(), ProvisionDigitalOutputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_DIGITAL_INPUT_OUTPUT_DEVICE:
			response = request(GSON.fromJson(wrapper.getMessage(), ProvisionDigitalInputOutputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_PWM_OUTPUT_DEVICE:
			response = request(GSON.fromJson(wrapper.getMessage(), ProvisionPwmOutputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_ANALOG_INPUT_DEVICE:
			response = request(GSON.fromJson(wrapper.getMessage(), ProvisionAnalogInputDevice.class));
			break;
		case MessageWrapperTypes.PROVISION_ANALOG_OUTPUT_DEVICE:
			response = request(GSON.fromJson(wrapper.getMessage(), ProvisionAnalogOutputDevice.class));
			break;
		case MessageWrapperTypes.GPIO_DIGITAL_READ:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioDigitalRead.class));
			break;
		case MessageWrapperTypes.GPIO_DIGITAL_WRITE:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioDigitalWrite.class));
			break;
		case MessageWrapperTypes.GPIO_PWM_READ:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioPwmRead.class));
			break;
		case MessageWrapperTypes.GPIO_PWM_WRITE:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioPwmWrite.class));
			break;
		case MessageWrapperTypes.GPIO_ANALOG_READ:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioAnalogRead.class));
			break;
		case MessageWrapperTypes.GPIO_ANALOG_WRITE:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioAnalogWrite.class));
			break;
		case MessageWrapperTypes.GPIO_EVENTS:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioEvents.class));
			break;
		case MessageWrapperTypes.GPIO_CLOSE:
			response = request(GSON.fromJson(wrapper.getMessage(), GpioClose.class));
			break;

		// I2C
		case MessageWrapperTypes.I2C_OPEN:
			response = request(GSON.fromJson(wrapper.getMessage(), I2COpen.class));
			break;
		case MessageWrapperTypes.I2C_PROBE:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CProbe.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_QUICK:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteQuick.class));
			break;
		case MessageWrapperTypes.I2C_READ_BYTE:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CReadByte.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_BYTE:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteByte.class));
			break;
		case MessageWrapperTypes.I2C_READ_BYTES:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CReadBytes.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_BYTES:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteBytes.class));
			break;
		case MessageWrapperTypes.I2C_READ_BYTE_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CReadByteData.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_BYTE_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteByteData.class));
			break;
		case MessageWrapperTypes.I2C_READ_WORD_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CReadWordData.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_WORD_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteWordData.class));
			break;
		case MessageWrapperTypes.I2C_PROCESS_CALL:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CProcessCall.class));
			break;
		case MessageWrapperTypes.I2C_READ_BLOCK_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CReadBlockData.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_BLOCK_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteBlockData.class));
			break;
		case MessageWrapperTypes.I2C_BLOCK_PROCESS_CALL:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CBlockProcessCall.class));
			break;
		case MessageWrapperTypes.I2C_READ_I2C_BLOCK_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CReadI2CBlockData.class));
			break;
		case MessageWrapperTypes.I2C_WRITE_I2C_BLOCK_DATA:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CWriteI2CBlockData.class));
			break;
		case MessageWrapperTypes.I2C_CLOSE:
			response = request(GSON.fromJson(wrapper.getMessage(), I2CClose.class));
			break;

		// SPI
		case MessageWrapperTypes.SPI_OPEN:
			response = request(GSON.fromJson(wrapper.getMessage(), SpiOpen.class));
			break;
		case MessageWrapperTypes.SPI_WRITE:
			response = request(GSON.fromJson(wrapper.getMessage(), SpiWrite.class));
			break;
		case MessageWrapperTypes.SPI_WRITE_AND_READ:
			response = request(GSON.fromJson(wrapper.getMessage(), SpiWriteAndRead.class));
			break;
		case MessageWrapperTypes.SPI_CLOSE:
			response = request(GSON.fromJson(wrapper.getMessage(), SpiClose.class));
			break;
		default:
			Logger.warn("Unhandled message type '{}'", wrapper.getType());
		}

		if (response != null) {
			sendMessage(session, response);
		}
	}

	@Override
	public void accept(DigitalInputEvent event) {
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
		MessageWrapper wrapper = new MessageWrapper(o.getClass().getSimpleName(), GSON.toJson(o));
		try {
			session.getRemote().sendString(GSON.toJson(wrapper));
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
			// TODO Cleanup this session?
		}
	}
}
