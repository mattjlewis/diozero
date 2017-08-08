package com.diozero.remote.mqtt;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - MQTT Server Host Process
 * Filename:     MqttServer.java  
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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.remote.BaseRemoteServer;
import com.diozero.remote.message.DiozeroProtos;
import com.diozero.remote.message.DiozeroProtosConverter;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiResponse;
import com.google.protobuf.InvalidProtocolBufferException;

public class MqttServer extends BaseRemoteServer implements MqttCallback {
	private static final String CLIENT_ID_PREFIX = "MQTT-PROVIDER-";

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <mqtt-url>", MqttServer.class.getName());
			System.exit(1);
		}

		try (MqttServer mqtt_server = new MqttServer(args[0])) {
			mqtt_server.start();
		} catch (UnknownHostException | MqttException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	private MqttClient mqttClient;
	private Object monitor;

	public MqttServer(String mqttUrl) throws UnknownHostException, MqttException {
		mqttClient = new MqttClient(mqttUrl, CLIENT_ID_PREFIX + InetAddress.getLocalHost().getHostName(),
				new MemoryPersistence());
		mqttClient.setCallback(this);
		MqttConnectOptions con_opts = new MqttConnectOptions();
		con_opts.setAutomaticReconnect(true);
		con_opts.setCleanSession(true);
		Logger.debug("Connecting to {}...", mqttUrl);
		mqttClient.connect(con_opts);
		Logger.debug("Connected to {}", mqttUrl);
	}

	@Override
	public void close() {
		synchronized (monitor) {
			monitor.notifyAll();
		}
		try {
			mqttClient.disconnect();
			mqttClient.close();
		} catch (MqttException e) {
			Logger.error(e, "Error: {}", e);
		}
		super.close();
	}

	public void start() throws MqttException {
		// Subscribe
		Logger.debug("Subscribing...");
		mqttClient.subscribe(MqttProviderConstants.GPIO_REQUEST_TOPIC + "/+");
		mqttClient.subscribe(MqttProviderConstants.SPI_REQUEST_TOPIC + "/+");
		mqttClient.subscribe(MqttProviderConstants.I2C_REQUEST_TOPIC + "/+");
		Logger.debug("Subscribed");

		monitor = new Object();
		try {
			// Wait forever
			synchronized (monitor) {
				monitor.wait();
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		Logger.warn(cause, "Lost connection to MQTT Server: {}", cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		try {
			switch (topic) {
			case MqttProviderConstants.GPIO_PROVISION_INPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionInput gpio_input = DiozeroProtos.Gpio.ProvisionInput.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(gpio_input)), gpio_input.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_PROVISION_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionOutput gpio_output = DiozeroProtos.Gpio.ProvisionOutput.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(gpio_output)), gpio_output.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_PROVISION_INPUT_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionInputOutput gpio_inout = DiozeroProtos.Gpio.ProvisionInputOutput.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(gpio_inout)), gpio_inout.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC:
				DiozeroProtos.Gpio.DigitalRead digital_read = DiozeroProtos.Gpio.DigitalRead.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(digital_read)), digital_read.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC:
				DiozeroProtos.Gpio.DigitalWrite digital_write = DiozeroProtos.Gpio.DigitalWrite.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(digital_write)), digital_write.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_EVENTS_TOPIC:
				DiozeroProtos.Gpio.Events gpio_events = DiozeroProtos.Gpio.Events.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(gpio_events)), gpio_events.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_CLOSE_TOPIC:
				DiozeroProtos.Gpio.Close gpio_close = DiozeroProtos.Gpio.Close.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(gpio_close)), gpio_close.getCorrelationId());
				break;
			case MqttProviderConstants.SPI_PROVISION_TOPIC:
				DiozeroProtos.Spi.Provision spi_open = DiozeroProtos.Spi.Provision.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(spi_open)), spi_open.getCorrelationId());
				break;
			case MqttProviderConstants.SPI_WRITE_TOPIC:
				DiozeroProtos.Spi.Write spi_write = DiozeroProtos.Spi.Write.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(spi_write)), spi_write.getCorrelationId());
				break;
			case MqttProviderConstants.SPI_WRITE_AND_READ_TOPIC:
				DiozeroProtos.Spi.WriteAndRead spi_write_and_read = DiozeroProtos.Spi.WriteAndRead.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(spi_write_and_read)), spi_write_and_read.getCorrelationId());
				break;
			case MqttProviderConstants.SPI_CLOSE_TOPIC:
				DiozeroProtos.Spi.Close spi_close = DiozeroProtos.Spi.Close.parseFrom(message.getPayload());
				publishResponse(processRequest(DiozeroProtosConverter.convert(spi_close)), spi_close.getCorrelationId());
				break;
			default:
				Logger.warn("Unrecognised topic '{}'", topic);
			}
		} catch (InvalidProtocolBufferException e) {
			Logger.error(e, "Invalid protobuf message: {}", e);
		} catch (MqttException e) {
			Logger.error(e, "MQTT error: {}", e);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}
	
	private void publishResponse(Response response, String correlationId) throws MqttException {
		DiozeroProtos.Response message = DiozeroProtosConverter.convert(response, correlationId);

		mqttClient.publish(MqttProviderConstants.RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(GpioDigitalReadResponse response, String correlationId) throws MqttException {
		DiozeroProtos.Gpio.DigitalReadResponse message = DiozeroProtosConverter.convert(response, correlationId);

		mqttClient.publish(MqttProviderConstants.RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(SpiResponse response, String correlationId) throws MqttException {
		DiozeroProtos.Spi.SpiResponse message = DiozeroProtosConverter.convert(response, correlationId);

		mqttClient.publish(MqttProviderConstants.RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	@Override
	public void valueChanged(DigitalInputEvent event) {
		DiozeroProtos.Gpio.Notification notification = DiozeroProtosConverter.convert(event);

		try {
			mqttClient.publish(MqttProviderConstants.GPIO_NOTIFICATION_TOPIC, notification.toByteArray(),
					MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
		} catch (MqttException e) {
			Logger.error(e, "MQTT Error: {}", e);
		}
	}
}
