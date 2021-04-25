package com.diozero.internal.provider.remote.mqtt;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     MqttTestApp.java
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.tinylog.Logger;

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionPwmOutputDevice;
import com.diozero.remote.mqtt.MqttProviderConstants;
import com.google.gson.Gson;

public class MqttTestApp implements MqttCallback {
	private static final String CLIENT_ID_PREFIX = "client";
	private static final Gson GSON = new Gson();

	private String mqttUrl = "tcp://leonard.home";
	private MqttClient mqttClient;

	public static void main(String[] args) throws UnknownHostException, MqttException {
		MqttTestApp app = new MqttTestApp();
		app.run();
	}

	public MqttTestApp() throws UnknownHostException, MqttException {
		mqttClient = new MqttClient(mqttUrl, CLIENT_ID_PREFIX + InetAddress.getLocalHost().getHostName(),
				new MemoryPersistence());
		mqttClient.setCallback(this);
		MqttConnectOptions con_opts = new MqttConnectOptions();
		con_opts.setAutomaticReconnect(true);
		con_opts.setCleanSession(true);
		Logger.debug("Connecting to {}...", mqttUrl);
		mqttClient.connect(con_opts);
		Logger.debug("Connected to {}", mqttUrl);

		mqttClient.subscribe("outTopic");
		mqttClient.subscribe(MqttProviderConstants.RESPONSE_TOPIC);
	}

	public void run() throws MqttPersistenceException, MqttException {
		MqttMessage message = new MqttMessage();

		int gpio = 16;

		ProvisionDigitalInputDevice input = new ProvisionDigitalInputDevice(gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH, UUID.randomUUID().toString());
		message.setPayload(GSON.toJson(input).getBytes());
		mqttClient.publish(MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_TOPIC, message);
		Logger.debug("Sent: {}", message);

		ProvisionDigitalOutputDevice output = new ProvisionDigitalOutputDevice(gpio, true, UUID.randomUUID().toString());
		message.setPayload(GSON.toJson(output).getBytes());
		mqttClient.publish(MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC, message);
		Logger.debug("Sent: {}", message);

		for (int i=0; i<5; i++) {
			GpioDigitalWrite write = new GpioDigitalWrite(gpio, true, UUID.randomUUID().toString());
			message.setPayload(GSON.toJson(write).getBytes());
			mqttClient.publish(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC, message);
			Logger.debug("Sent: {}", message);
			sleep(1000);

			write = new GpioDigitalWrite(gpio, false, UUID.randomUUID().toString());
			message.setPayload(GSON.toJson(write).getBytes());
			mqttClient.publish(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC, message);
			Logger.debug("Sent: {}", message);
			sleep(1000);
		}

		ProvisionPwmOutputDevice pwm = new ProvisionPwmOutputDevice(gpio, 1000, 0.1f, UUID.randomUUID().toString());
		message.setPayload(GSON.toJson(pwm).getBytes());
		mqttClient.publish(MqttProviderConstants.GPIO_PROVISION_PWM_OUTPUT_TOPIC, message);
		Logger.debug("Sent: {}", message);
		
		for (float f=0; f<1; f+=0.01) {
			GpioPwmWrite pwm_write = new GpioPwmWrite(gpio, f, UUID.randomUUID().toString());
			message.setPayload(GSON.toJson(pwm_write).getBytes());
			mqttClient.publish(MqttProviderConstants.GPIO_PWM_WRITE_TOPIC, message);
			Logger.debug("Sent: {}", message);
			sleep(50);
		}
		for (float f=1; f>0; f-=0.01) {
			GpioPwmWrite pwm_write = new GpioPwmWrite(gpio, f, UUID.randomUUID().toString());
			message.setPayload(GSON.toJson(pwm_write).getBytes());
			mqttClient.publish(MqttProviderConstants.GPIO_PWM_WRITE_TOPIC, message);
			Logger.debug("Sent: {}", message);
			sleep(50);
		}

		sleep(1000);

		mqttClient.disconnect();
		mqttClient.close();
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		Logger.debug("{}: {}", topic, message);
	}
	
	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}
