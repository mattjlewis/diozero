package com.diozero.remote.server.mqtt;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     MqttProtobufServer.java  
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
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.I2CReadByteResponse;
import com.diozero.remote.message.I2CReadResponse;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.protobuf.DiozeroProtos;
import com.diozero.remote.server.BaseRemoteServer;
import com.google.protobuf.InvalidProtocolBufferException;

public class MqttProtobufServer extends BaseRemoteServer implements MqttCallback {
	private static final String CLIENT_ID_PREFIX = "MQTT-PROVIDER-";

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <mqtt-url>", MqttProtobufServer.class.getName());
			System.exit(1);
		}

		try (MqttProtobufServer mqtt_server = new MqttProtobufServer(args[0])) {
			mqtt_server.start();
		} catch (UnknownHostException | MqttException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	private MqttClient mqttClient;
	private Object monitor;

	public MqttProtobufServer(String mqttUrl) throws UnknownHostException, MqttException {
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
			// GPIO
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionDigitalInput digital_input = DiozeroProtos.Gpio.ProvisionDigitalInput
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_input)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionDigitalOutput digital_output = DiozeroProtos.Gpio.ProvisionDigitalOutput
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_output)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionDigitalInputOutput digital_inout = DiozeroProtos.Gpio.ProvisionDigitalInputOutput
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_inout)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_PWM_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionPwmOutput pwm_output = DiozeroProtos.Gpio.ProvisionPwmOutput
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_output)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_ANALOG_INPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionAnalogInput analog_input = DiozeroProtos.Gpio.ProvisionAnalogInput
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_input)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_ANALOG_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionAnalogOutput analog_output = DiozeroProtos.Gpio.ProvisionAnalogOutput
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_output)));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC:
				DiozeroProtos.Gpio.DigitalRead digital_read = DiozeroProtos.Gpio.DigitalRead
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_read)));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC:
				DiozeroProtos.Gpio.DigitalWrite digital_write = DiozeroProtos.Gpio.DigitalWrite
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_write)));
				break;
			case MqttProviderConstants.GPIO_PWM_READ_TOPIC:
				DiozeroProtos.Gpio.PwmRead pwm_read = DiozeroProtos.Gpio.PwmRead
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_read)));
				break;
			case MqttProviderConstants.GPIO_PWM_WRITE_TOPIC:
				DiozeroProtos.Gpio.PwmWrite pwm_write = DiozeroProtos.Gpio.PwmWrite
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_write)));
				break;
			case MqttProviderConstants.GPIO_ANALOG_READ_TOPIC:
				DiozeroProtos.Gpio.AnalogRead analog_read = DiozeroProtos.Gpio.AnalogRead
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_read)));
				break;
			case MqttProviderConstants.GPIO_ANALOG_WRITE_TOPIC:
				DiozeroProtos.Gpio.AnalogWrite analog_write = DiozeroProtos.Gpio.AnalogWrite
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_write)));
				break;
			case MqttProviderConstants.GPIO_EVENTS_TOPIC:
				DiozeroProtos.Gpio.Events gpio_events = DiozeroProtos.Gpio.Events.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(gpio_events)));
				break;
			case MqttProviderConstants.GPIO_CLOSE_TOPIC:
				DiozeroProtos.Gpio.Close gpio_close = DiozeroProtos.Gpio.Close.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(gpio_close)));
				break;

			// I2C
			case MqttProviderConstants.I2C_OPEN_TOPIC:
				DiozeroProtos.I2C.Open i2c_open = DiozeroProtos.I2C.Open.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_open)));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_TOPIC:
				DiozeroProtos.I2C.ReadByte i2c_read_byte = DiozeroProtos.I2C.ReadByte.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_byte)));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTE_TOPIC:
				DiozeroProtos.I2C.WriteByte i2c_write_byte = DiozeroProtos.I2C.WriteByte
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_byte)));
				break;
			case MqttProviderConstants.I2C_READ_TOPIC:
				DiozeroProtos.I2C.Read i2c_read = DiozeroProtos.I2C.Read.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read)));
				break;
			case MqttProviderConstants.I2C_WRITE_TOPIC:
				DiozeroProtos.I2C.Write i2c_write = DiozeroProtos.I2C.Write.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write)));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_DATA_TOPIC:
				DiozeroProtos.I2C.ReadByteData i2c_read_byte_data = DiozeroProtos.I2C.ReadByteData
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_byte_data)));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTE_DATA_TOPIC:
				DiozeroProtos.I2C.WriteByteData i2c_write_byte_data = DiozeroProtos.I2C.WriteByteData
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_byte_data)));
				break;
			case MqttProviderConstants.I2C_READ_I2C_BLOCK_DATA_TOPIC:
				DiozeroProtos.I2C.ReadI2CBlockData i2c_read_i2c_block_data = DiozeroProtos.I2C.ReadI2CBlockData
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_i2c_block_data)));
				break;
			case MqttProviderConstants.I2C_WRITE_I2C_BLOCK_DATA_TOPIC:
				DiozeroProtos.I2C.WriteI2CBlockData i2c_write_i2c_block_data = DiozeroProtos.I2C.WriteI2CBlockData
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_i2c_block_data)));
				break;
			case MqttProviderConstants.I2C_CLOSE_TOPIC:
				DiozeroProtos.I2C.Close i2c_close = DiozeroProtos.I2C.Close.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_close)));
				break;
			
			// SPI
			case MqttProviderConstants.SPI_OPEN_TOPIC:
				DiozeroProtos.Spi.Open spi_open = DiozeroProtos.Spi.Open.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_open)));
				break;
			case MqttProviderConstants.SPI_WRITE_TOPIC:
				DiozeroProtos.Spi.Write spi_write = DiozeroProtos.Spi.Write.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_write)));
				break;
			case MqttProviderConstants.SPI_WRITE_AND_READ_TOPIC:
				DiozeroProtos.Spi.WriteAndRead spi_write_and_read = DiozeroProtos.Spi.WriteAndRead
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_write_and_read)));
				break;
			case MqttProviderConstants.SPI_CLOSE_TOPIC:
				DiozeroProtos.Spi.Close spi_close = DiozeroProtos.Spi.Close.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_close)));
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

	private void publishResponse(Response response) throws MqttException {
		DiozeroProtos.Response message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(GpioDigitalReadResponse response) throws MqttException {
		DiozeroProtos.Gpio.DigitalReadResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.GPIO_DIGITAL_READ_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(GpioPwmReadResponse response) throws MqttException {
		DiozeroProtos.Gpio.PwmReadResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.GPIO_PWM_READ_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(GpioAnalogReadResponse response) throws MqttException {
		DiozeroProtos.Gpio.AnalogReadResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.GPIO_ANALOG_READ_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CReadByteResponse response) throws MqttException {
		DiozeroProtos.I2C.ReadByteResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_READ_BYTE_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CReadResponse response) throws MqttException {
		DiozeroProtos.I2C.ReadResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_READ_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(SpiResponse response) throws MqttException {
		DiozeroProtos.Spi.SpiResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.SPI_RXDATA_RESPONSE_TOPIC, message.toByteArray(),
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
