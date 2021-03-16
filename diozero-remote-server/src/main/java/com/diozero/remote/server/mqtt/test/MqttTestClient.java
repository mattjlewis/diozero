package com.diozero.remote.server.mqtt.test;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     MqttTestClient.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.remote.message.protobuf.DiozeroProtos;
import com.diozero.remote.mqtt.MqttProviderConstants;
import com.diozero.remote.server.mqtt.MqttProtobufServer;
import com.google.protobuf.GeneratedMessageV3;

public class MqttTestClient implements AutoCloseable, MqttCallback {
	private static final long TIMEOUT_MS = 1000;

	public static void main(String[] args) {

		if (args.length < 1) {
			Logger.error("Usage: {} <MQTT-url>", MqttProtobufServer.class.getName());
			System.exit(1);
		}

		try (MqttTestClient mqtt_client = new MqttTestClient(args[0])) {
			mqtt_client.sendTestMessages(16);
		} catch (MqttException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	private MqttClient mqttClient;
	private ReentrantLock lock;
	private Map<String, Condition> conditions;
	private Map<String, DiozeroProtos.Response> responses;

	public MqttTestClient(String mqttUrl) throws MqttException {
		mqttClient = new MqttClient(mqttUrl, MqttClient.generateClientId(), new MemoryPersistence());
		mqttClient.setCallback(this);
		MqttConnectOptions con_opts = new MqttConnectOptions();
		con_opts.setAutomaticReconnect(true);
		con_opts.setCleanSession(true);
		mqttClient.connect(con_opts);
		Logger.debug("Connected to {}", mqttUrl);

		lock = new ReentrantLock();
		conditions = new HashMap<>();
		responses = new HashMap<>();

		// Subscribe
		Logger.debug("Subscribing to {}...", MqttProviderConstants.RESPONSE_TOPIC);
		mqttClient.subscribe(MqttProviderConstants.RESPONSE_TOPIC);
		Logger.debug("Subscribed");
	}

	@Override
	public void close() {
		try {
			mqttClient.disconnect();
			mqttClient.close();
		} catch (MqttException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		Logger.warn(cause, "Lost connection to MQTT Server: {}", cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		if (topic.equals(MqttProviderConstants.RESPONSE_TOPIC)) {
			// TODO How to handle different response types?
			DiozeroProtos.Response response = DiozeroProtos.Response.parseFrom(message.getPayload());
			Logger.info("Got response message: {}", response);

			String correlation_id = response.getCorrelationId();
			responses.put(correlation_id, response);

			Condition condition = conditions.remove(correlation_id);
			if (condition == null) {
				Logger.error("No condition for correlation id {}", correlation_id);
			} else {
				lock.lock();
				try {
					condition.signalAll();
				} finally {
					lock.unlock();
				}
			}
		} else {
			Logger.warn("Unrecognised topic {}", topic);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		Logger.debug("Delivered message {}", token);
	}

	private void sendTestMessages(int gpio) throws MqttException {
		try {
			DiozeroProtos.Gpio.ProvisionDigitalOutputRequest digital_output = createGpioProvisionDigitalOutput(gpio,
					true);
			DiozeroProtos.Response response = sendMessage(MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC,
					digital_output.getCorrelationId(), digital_output);
			Logger.debug("Got response to provision output: {}", response);
			Thread.sleep(500);

			DiozeroProtos.Gpio.DigitalReadRequest gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			DiozeroProtos.Gpio.DigitalWriteRequest gpio_write = createGpioDigitalWrite(gpio, false);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC, gpio_write.getCorrelationId(),
					gpio_write);
			Logger.debug("Got response to output: {}", response);
			Thread.sleep(500);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			DiozeroProtos.Gpio.CloseRequest gpio_close = createGpioClose(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_CLOSE_TOPIC, gpio_close.getCorrelationId(), gpio_close);
			Logger.debug("Got response to close: {}", response);

			DiozeroProtos.Gpio.ProvisionDigitalInputRequest digital_input = createGpioProvisionDigitalInput(gpio,
					DiozeroProtos.Gpio.PullUpDown.PUD_NONE, DiozeroProtos.Gpio.Trigger.TRIGGER_BOTH);
			response = sendMessage(MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_TOPIC,
					digital_input.getCorrelationId(), digital_input);
			Logger.debug("Got response to input: {}", response);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			gpio_close = createGpioClose(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_CLOSE_TOPIC, gpio_close.getCorrelationId(), gpio_close);
			Logger.debug("Got response to close: {}", response);

			digital_output = createGpioProvisionDigitalOutput(gpio, true);
			response = sendMessage(MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC,
					digital_output.getCorrelationId(), digital_output);
			Logger.debug("Got response to output: {}", response);
			Thread.sleep(500);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			gpio_write = createGpioDigitalWrite(gpio, false);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC, gpio_write.getCorrelationId(),
					gpio_write);
			Logger.debug("Got response to output: {}", response);
			Thread.sleep(500);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			gpio_close = createGpioClose(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_CLOSE_TOPIC, gpio_close.getCorrelationId(), gpio_close);
			Logger.debug("Got response to close: {}", response);

			DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest gpio_inout = createGpioProvisionDigitalInputOutput(
					gpio, true);
			response = sendMessage(MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_TOPIC,
					gpio_inout.getCorrelationId(), gpio_inout);
			Logger.debug("Got response to inout: {}", response);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			gpio_write = createGpioDigitalWrite(gpio, true);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC, gpio_write.getCorrelationId(),
					gpio_write);
			Logger.debug("Got response to output: {}", response);
			Thread.sleep(500);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			gpio_write = createGpioDigitalWrite(gpio, false);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC, gpio_write.getCorrelationId(),
					gpio_write);
			Logger.debug("Got response to output: {}", response);
			Thread.sleep(500);

			gpio_read = createGpioDigitalRead(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC, gpio_read.getCorrelationId(),
					gpio_read);
			Logger.debug("Got response to read: {}", response);

			gpio_close = createGpioClose(gpio);
			response = sendMessage(MqttProviderConstants.GPIO_CLOSE_TOPIC, gpio_close.getCorrelationId(), gpio_close);
			Logger.debug("Got response to close: {}", response);
		} catch (InterruptedException e) {
			Logger.error(e, "Interrupted: {}", e);
		}
	}

	public DiozeroProtos.Response sendMessage(String topic, String correlationId, GeneratedMessageV3 message)
			throws MqttException {
		Condition condition = lock.newCondition();
		conditions.put(correlationId, condition);

		lock.lock();
		try {
			mqttClient.publish(topic, message.toByteArray(), MqttProviderConstants.DEFAULT_QOS,
					MqttProviderConstants.DEFAULT_RETAINED);

			Logger.info("Waiting for response...");
			condition.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Logger.error(e, "Interrupted: {}", e);
		} finally {
			lock.unlock();
		}

		DiozeroProtos.Response response = responses.remove(correlationId);
		if (response == null) {
			throw new RuntimeIOException("Cannot find response message for " + correlationId);
		}

		return response;
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalOutputRequest createGpioProvisionDigitalOutput(int gpio,
			boolean initialValue) {
		return DiozeroProtos.Gpio.ProvisionDigitalOutputRequest.newBuilder()
				.setCorrelationId(UUID.randomUUID().toString()).setGpio(gpio).setInitialValue(initialValue).build();
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalInputRequest createGpioProvisionDigitalInput(int gpio,
			DiozeroProtos.Gpio.PullUpDown pud, DiozeroProtos.Gpio.Trigger trigger) {
		return DiozeroProtos.Gpio.ProvisionDigitalInputRequest.newBuilder()
				.setCorrelationId(UUID.randomUUID().toString()).setGpio(gpio).setPud(pud).setTrigger(trigger).build();
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest createGpioProvisionDigitalInputOutput(int gpio,
			boolean output) {
		return DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest.newBuilder()
				.setCorrelationId(UUID.randomUUID().toString()).setGpio(gpio).setOutput(output).build();
	}

	public static DiozeroProtos.Gpio.DigitalWriteRequest createGpioDigitalWrite(int gpio, boolean value) {
		return DiozeroProtos.Gpio.DigitalWriteRequest.newBuilder().setCorrelationId(UUID.randomUUID().toString())
				.setGpio(gpio).setValue(value).build();
	}

	public static DiozeroProtos.Gpio.DigitalReadRequest createGpioDigitalRead(int gpio) {
		return DiozeroProtos.Gpio.DigitalReadRequest.newBuilder().setCorrelationId(UUID.randomUUID().toString())
				.setGpio(gpio).build();
	}

	public static DiozeroProtos.Gpio.CloseRequest createGpioClose(int gpio) {
		return DiozeroProtos.Gpio.CloseRequest.newBuilder().setCorrelationId(UUID.randomUUID().toString()).setGpio(gpio)
				.build();
	}
}
