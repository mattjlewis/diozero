package com.diozero.internal.provider.remote.mqtt;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     ProtobufMqttProtocolHandler.java  
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

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.pmw.tinylog.Logger;

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
import com.diozero.remote.mqtt.MqttProviderConstants;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RuntimeIOException;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;

public class ProtobufMqttProtocolHandler extends ProtobufBaseAsyncProtocolHandler implements MqttCallback {
	private static final String MQTT_URL_PROP = "MQTT_PROVIDER_URL";

	private MqttClient mqttClient;

	public ProtobufMqttProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
		super(deviceFactory);

		String mqtt_url = PropertyUtil.getProperty(MQTT_URL_PROP, null);
		if (mqtt_url == null) {
			throw new RuntimeIOException("Property '" + MQTT_URL_PROP + "' must be set");
		}

		try {
			mqttClient = new MqttClient(mqtt_url, MqttClient.generateClientId(), new MemoryPersistence());
			mqttClient.setCallback(this);
			MqttConnectOptions con_opts = new MqttConnectOptions();
			con_opts.setAutomaticReconnect(true);
			con_opts.setCleanSession(true);
			mqttClient.connect(con_opts);
			Logger.debug("Connected to {}", mqtt_url);

			// Subscribe
			Logger.debug("Subscribing to response and notification topics...");
			mqttClient.subscribe(MqttProviderConstants.RESPONSE_TOPIC);
			mqttClient.subscribe(MqttProviderConstants.GPIO_NOTIFICATION_TOPIC);
			Logger.debug("Subscribed");
		} catch (MqttException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void close() {
		Logger.info("close()");
		if (mqttClient != null) {
			try {
				mqttClient.disconnect();
				mqttClient.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	protected void sendMessage(String topic, GeneratedMessageV3 message) throws MqttException {
		mqttClient.publish(topic, message.toByteArray(), MqttProviderConstants.DEFAULT_QOS,
				MqttProviderConstants.DEFAULT_RETAINED);
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_INPUT_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(ProvisionDigitalOutputDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_OUTPUT_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputOutputDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_INPUT_OUTPUT_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public GpioDigitalReadResponse sendRequest(GpioDigitalRead request) {
		String correlation_id = UUID.randomUUID().toString();
		return (GpioDigitalReadResponse) requestResponse(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(GpioDigitalWrite request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(GpioEvents request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.GPIO_EVENTS_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(GpioClose request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.GPIO_CLOSE_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(ProvisionSpiDevice request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.SPI_PROVISION_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(SpiWrite request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.SPI_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public SpiResponse sendRequest(SpiWriteAndRead request) {
		String correlation_id = UUID.randomUUID().toString();
		return (SpiResponse) requestResponse(MqttProviderConstants.SPI_WRITE_AND_READ_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public Response sendRequest(SpiClose request) {
		String correlation_id = UUID.randomUUID().toString();
		return requestResponse(MqttProviderConstants.SPI_CLOSE_TOPIC,
				DiozeroProtosConverter.convert(request, correlation_id), correlation_id);
	}

	@Override
	public void connectionLost(Throwable cause) {
		Logger.warn(cause, "Lost MQTT connection: {}", cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		Logger.debug("topic: {}", topic);

		try {
			switch (topic) {
			case MqttProviderConstants.RESPONSE_TOPIC:
				DiozeroProtos.Response response = DiozeroProtos.Response.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(response), response.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_DIGITAL_READ_RESPONSE_TOPIC:
				DiozeroProtos.Gpio.DigitalReadResponse digital_read_response = DiozeroProtos.Gpio.DigitalReadResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(digital_read_response),
						digital_read_response.getCorrelationId());
				break;
			case MqttProviderConstants.SPI_RXDATA_RESPONSE_TOPIC:
				DiozeroProtos.Spi.SpiResponse spi_response = DiozeroProtos.Spi.SpiResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(spi_response), spi_response.getCorrelationId());
				break;
			case MqttProviderConstants.GPIO_NOTIFICATION_TOPIC:
				processEvent(DiozeroProtosConverter
						.convert(DiozeroProtos.Gpio.Notification.parseFrom(message.getPayload())));
				break;
			default:
				Logger.warn("Unrecognised topic {}", topic);
			}
		} catch (InvalidProtocolBufferException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// Ignore
	}
}
