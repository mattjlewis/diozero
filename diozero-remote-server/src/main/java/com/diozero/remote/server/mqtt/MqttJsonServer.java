package com.diozero.remote.server.mqtt;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     MqttJsonServer.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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
import com.diozero.remote.message.DiozeroProtosConverter;
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
import com.diozero.remote.message.protobuf.DiozeroProtos;
import com.diozero.remote.server.BaseRemoteServer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class MqttJsonServer extends BaseRemoteServer implements MqttCallback {
	private static final String CLIENT_ID_PREFIX = "MQTT-PROVIDER-";
	private static final Gson GSON = new Gson();

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <mqtt-url>", MqttJsonServer.class.getName());
			System.exit(1);
		}

		try (MqttJsonServer mqtt_server = new MqttJsonServer(args[0])) {
			mqtt_server.start();
		} catch (UnknownHostException | MqttException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	private MqttClient mqttClient;
	private Object monitor;

	public MqttJsonServer(String mqttUrl) throws UnknownHostException, MqttException {
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
		mqttClient.subscribe(MqttProviderConstants.I2C_REQUEST_TOPIC + "/+");
		mqttClient.subscribe(MqttProviderConstants.SPI_REQUEST_TOPIC + "/+");
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
			// GPIO
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_TOPIC:
				ProvisionDigitalInputDevice digital_input = GSON.fromJson(new String(message.getPayload()),
						ProvisionDigitalInputDevice.class);
				publishResponse(request(digital_input));
				break;
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC:
				ProvisionDigitalOutputDevice digital_output = GSON.fromJson(new String(message.getPayload()),
						ProvisionDigitalOutputDevice.class);
				publishResponse(request(digital_output));
				break;
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_TOPIC:
				ProvisionDigitalInputOutputDevice digital_inout = GSON.fromJson(new String(message.getPayload()),
						ProvisionDigitalInputOutputDevice.class);
				publishResponse(request(digital_inout));
				break;
			case MqttProviderConstants.GPIO_PROVISION_PWM_OUTPUT_TOPIC:
				ProvisionPwmOutputDevice pwm_output = GSON.fromJson(new String(message.getPayload()),
						ProvisionPwmOutputDevice.class);
				publishResponse(request(pwm_output));
				break;
			case MqttProviderConstants.GPIO_PROVISION_ANALOG_INPUT_TOPIC:
				ProvisionAnalogInputDevice analog_input = GSON.fromJson(new String(message.getPayload()),
						ProvisionAnalogInputDevice.class);
				publishResponse(request(analog_input));
				break;
			case MqttProviderConstants.GPIO_PROVISION_ANALOG_OUTPUT_TOPIC:
				ProvisionAnalogOutputDevice analog_output = GSON.fromJson(new String(message.getPayload()),
						ProvisionAnalogOutputDevice.class);
				publishResponse(request(analog_output));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC:
				GpioDigitalRead digital_read = GSON.fromJson(new String(message.getPayload()), GpioDigitalRead.class);
				publishResponse(request(digital_read));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC:
				GpioDigitalWrite digital_write = GSON.fromJson(new String(message.getPayload()), GpioDigitalWrite.class);
				publishResponse(request(digital_write));
				break;
			case MqttProviderConstants.GPIO_PWM_READ_TOPIC:
				GpioPwmRead pwm_read = GSON.fromJson(new String(message.getPayload()), GpioPwmRead.class);
				publishResponse(request(pwm_read));
				break;
			case MqttProviderConstants.GPIO_PWM_WRITE_TOPIC:
				GpioPwmWrite pwm_write = GSON.fromJson(new String(message.getPayload()), GpioPwmWrite.class);
				publishResponse(request(pwm_write));
				break;
			case MqttProviderConstants.GPIO_ANALOG_READ_TOPIC:
				GpioAnalogRead analog_read = GSON.fromJson(new String(message.getPayload()), GpioAnalogRead.class);
				publishResponse(request(analog_read));
				break;
			case MqttProviderConstants.GPIO_ANALOG_WRITE_TOPIC:
				GpioAnalogWrite analog_write = GSON.fromJson(new String(message.getPayload()), GpioAnalogWrite.class);
				publishResponse(request(analog_write));
				break;
			case MqttProviderConstants.GPIO_EVENTS_TOPIC:
				GpioEvents gpio_events = GSON.fromJson(new String(message.getPayload()), GpioEvents.class);
				publishResponse(request(gpio_events));
				break;
			case MqttProviderConstants.GPIO_CLOSE_TOPIC:
				GpioClose gpio_close = GSON.fromJson(new String(message.getPayload()), GpioClose.class);
				publishResponse(request(gpio_close));
				break;

			// I2C
			case MqttProviderConstants.I2C_OPEN_TOPIC:
				I2COpen i2c_open = GSON.fromJson(new String(message.getPayload()), I2COpen.class);
				publishResponse(request(i2c_open));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_TOPIC:
				I2CReadByte i2c_read_byte = GSON.fromJson(new String(message.getPayload()), I2CReadByte.class);
				publishResponse(request(i2c_read_byte));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTE_TOPIC:
				I2CWriteByte i2c_write_byte = GSON.fromJson(new String(message.getPayload()), I2CWriteByte.class);
				publishResponse(request(i2c_write_byte));
				break;
			case MqttProviderConstants.I2C_READ_TOPIC:
				I2CRead i2c_read = GSON.fromJson(new String(message.getPayload()), I2CRead.class);
				publishResponse(request(i2c_read));
				break;
			case MqttProviderConstants.I2C_WRITE_TOPIC:
				I2CWrite i2c_write = GSON.fromJson(new String(message.getPayload()), I2CWrite.class);
				publishResponse(request(i2c_write));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_DATA_TOPIC:
				I2CReadByteData i2c_read_byte_data = GSON.fromJson(new String(message.getPayload()), I2CReadByteData.class);
				publishResponse(request(i2c_read_byte_data));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTE_DATA_TOPIC:
				I2CWriteByteData i2c_write_byte_data = GSON.fromJson(new String(message.getPayload()), I2CWriteByteData.class);
				publishResponse(request(i2c_write_byte_data));
				break;
			case MqttProviderConstants.I2C_READ_I2C_BLOCK_DATA_TOPIC:
				I2CReadI2CBlockData i2c_read_i2c_block_data = GSON.fromJson(new String(message.getPayload()), I2CReadI2CBlockData.class);
				publishResponse(request(i2c_read_i2c_block_data));
				break;
			case MqttProviderConstants.I2C_WRITE_I2C_BLOCK_DATA_TOPIC:
				I2CWriteI2CBlockData i2c_write_i2c_block_data = GSON.fromJson(new String(message.getPayload()), I2CWriteI2CBlockData.class);
				publishResponse(request(i2c_write_i2c_block_data));
				break;
			case MqttProviderConstants.I2C_CLOSE_TOPIC:
				I2CClose i2c_close = GSON.fromJson(new String(message.getPayload()), I2CClose.class);
				publishResponse(request(i2c_close));
				break;

			// SPI
			case MqttProviderConstants.SPI_OPEN_TOPIC:
				SpiOpen spi_open = GSON.fromJson(new String(message.getPayload()), SpiOpen.class);
				publishResponse(request(spi_open));
				break;
			case MqttProviderConstants.SPI_WRITE_TOPIC:
				SpiWrite spi_write = GSON.fromJson(new String(message.getPayload()), SpiWrite.class);
				publishResponse(request(spi_write));
				break;
			case MqttProviderConstants.SPI_WRITE_AND_READ_TOPIC:
				SpiWriteAndRead spi_write_and_read = GSON.fromJson(new String(message.getPayload()), SpiWriteAndRead.class);
				publishResponse(request(spi_write_and_read));
				break;
			case MqttProviderConstants.SPI_CLOSE_TOPIC:
				SpiClose spi_close = GSON.fromJson(new String(message.getPayload()), SpiClose.class);
				publishResponse(request(spi_close));
				break;
			default:
				Logger.warn("Unrecognised topic '{}'", topic);
			}
		} catch (JsonSyntaxException e) {
			Logger.error(e, "Invalid JSON message: {}", e);
		} catch (MqttException e) {
			Logger.error(e, "MQTT error: {}", e);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}
	
	private void publishResponse(Response response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(GpioDigitalReadResponse response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.GPIO_DIGITAL_READ_RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(GpioPwmReadResponse response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.GPIO_PWM_READ_RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(GpioAnalogReadResponse response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.GPIO_ANALOG_READ_RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(I2CReadByteResponse response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.I2C_READ_BYTE_RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(I2CReadResponse response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.I2C_READ_RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}
	
	private void publishResponse(SpiResponse response) throws MqttException {
		mqttClient.publish(MqttProviderConstants.SPI_RXDATA_RESPONSE_TOPIC, GSON.toJson(response).getBytes(),
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
