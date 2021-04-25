package com.diozero.remote.server.mqtt;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     MqttProtobufServer.java
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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.RuntimeIOException;
import com.diozero.remote.message.DiozeroProtosConverter;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioGetPwmFrequencyResponse;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.I2CBooleanResponse;
import com.diozero.remote.message.I2CByteResponse;
import com.diozero.remote.message.I2CBytesResponse;
import com.diozero.remote.message.I2CReadBlockDataResponse;
import com.diozero.remote.message.I2CWordResponse;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.protobuf.DiozeroProtos;
import com.diozero.remote.mqtt.MqttProviderConstants;
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

	@Override
	public void start() {
		try {
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
		} catch (MqttException e) {
			throw new RuntimeIOException(e);
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
				DiozeroProtos.Gpio.ProvisionDigitalInputRequest digital_input = DiozeroProtos.Gpio.ProvisionDigitalInputRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_input)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionDigitalOutputRequest digital_output = DiozeroProtos.Gpio.ProvisionDigitalOutputRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_output)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest digital_inout = DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_inout)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_PWM_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionPwmOutputRequest pwm_output = DiozeroProtos.Gpio.ProvisionPwmOutputRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_output)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_ANALOG_INPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionAnalogInputRequest analog_input = DiozeroProtos.Gpio.ProvisionAnalogInputRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_input)));
				break;
			case MqttProviderConstants.GPIO_PROVISION_ANALOG_OUTPUT_TOPIC:
				DiozeroProtos.Gpio.ProvisionAnalogOutputRequest analog_output = DiozeroProtos.Gpio.ProvisionAnalogOutputRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_output)));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC:
				DiozeroProtos.Gpio.DigitalReadRequest digital_read = DiozeroProtos.Gpio.DigitalReadRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_read)));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC:
				DiozeroProtos.Gpio.DigitalWriteRequest digital_write = DiozeroProtos.Gpio.DigitalWriteRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(digital_write)));
				break;
			case MqttProviderConstants.GPIO_PWM_READ_TOPIC:
				DiozeroProtos.Gpio.PwmReadRequest pwm_read = DiozeroProtos.Gpio.PwmReadRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_read)));
				break;
			case MqttProviderConstants.GPIO_PWM_WRITE_TOPIC:
				DiozeroProtos.Gpio.PwmWriteRequest pwm_write = DiozeroProtos.Gpio.PwmWriteRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_write)));
				break;
			case MqttProviderConstants.GPIO_GET_PWM_FREQUENCY_TOPIC:
				DiozeroProtos.Gpio.GetPwmFrequencyRequest pwm_frequency = DiozeroProtos.Gpio.GetPwmFrequencyRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(pwm_frequency)));
				break;
			case MqttProviderConstants.GPIO_SET_PWM_FREQUENCY_TOPIC:
				DiozeroProtos.Gpio.SetPwmFrequencyRequest set_pwm_frequency = DiozeroProtos.Gpio.SetPwmFrequencyRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(set_pwm_frequency)));
				break;
			case MqttProviderConstants.GPIO_ANALOG_READ_TOPIC:
				DiozeroProtos.Gpio.AnalogReadRequest analog_read = DiozeroProtos.Gpio.AnalogReadRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_read)));
				break;
			case MqttProviderConstants.GPIO_ANALOG_WRITE_TOPIC:
				DiozeroProtos.Gpio.AnalogWriteRequest analog_write = DiozeroProtos.Gpio.AnalogWriteRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(analog_write)));
				break;
			case MqttProviderConstants.GPIO_EVENTS_TOPIC:
				DiozeroProtos.Gpio.EventsRequest gpio_events = DiozeroProtos.Gpio.EventsRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(gpio_events)));
				break;
			case MqttProviderConstants.GPIO_CLOSE_TOPIC:
				DiozeroProtos.Gpio.CloseRequest gpio_close = DiozeroProtos.Gpio.CloseRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(gpio_close)));
				break;

			// I2C
			case MqttProviderConstants.I2C_OPEN_TOPIC:
				DiozeroProtos.I2C.OpenRequest i2c_open = DiozeroProtos.I2C.OpenRequest.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_open)));
				break;
			case MqttProviderConstants.I2C_PROBE_TOPIC:
				DiozeroProtos.I2C.ProbeRequest i2c_probe = DiozeroProtos.I2C.ProbeRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_probe)));
				break;
			case MqttProviderConstants.I2C_WRITE_QUICK_TOPIC:
				DiozeroProtos.I2C.WriteQuickRequest i2c_write_quick = DiozeroProtos.I2C.WriteQuickRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_quick)));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_TOPIC:
				DiozeroProtos.I2C.ReadByteRequest i2c_read_byte = DiozeroProtos.I2C.ReadByteRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_byte)));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTE_TOPIC:
				DiozeroProtos.I2C.WriteByteRequest i2c_write_byte = DiozeroProtos.I2C.WriteByteRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_byte)));
				break;
			case MqttProviderConstants.I2C_READ_BYTES_TOPIC:
				DiozeroProtos.I2C.ReadBytesRequest i2c_read = DiozeroProtos.I2C.ReadBytesRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read)));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTES_TOPIC:
				DiozeroProtos.I2C.WriteBytesRequest i2c_write = DiozeroProtos.I2C.WriteBytesRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write)));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_DATA_TOPIC:
				DiozeroProtos.I2C.ReadByteDataRequest i2c_read_byte_data = DiozeroProtos.I2C.ReadByteDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_byte_data)));
				break;
			case MqttProviderConstants.I2C_WRITE_BYTE_DATA_TOPIC:
				DiozeroProtos.I2C.WriteByteDataRequest i2c_write_byte_data = DiozeroProtos.I2C.WriteByteDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_byte_data)));
				break;
			case MqttProviderConstants.I2C_READ_WORD_DATA_TOPIC:
				DiozeroProtos.I2C.ReadWordDataRequest i2c_read_word_data = DiozeroProtos.I2C.ReadWordDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_word_data)));
				break;
			case MqttProviderConstants.I2C_WRITE_WORD_DATA_TOPIC:
				DiozeroProtos.I2C.WriteWordDataRequest i2c_write_word_data = DiozeroProtos.I2C.WriteWordDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_word_data)));
				break;
			case MqttProviderConstants.I2C_PROCESS_CALL_TOPIC:
				DiozeroProtos.I2C.ProcessCallRequest i2c_process_call_data = DiozeroProtos.I2C.ProcessCallRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_process_call_data)));
				break;
			case MqttProviderConstants.I2C_READ_BLOCK_DATA_TOPIC:
				DiozeroProtos.I2C.ReadBlockDataRequest i2c_read_block_data = DiozeroProtos.I2C.ReadBlockDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_block_data)));
				break;
			case MqttProviderConstants.I2C_WRITE_BLOCK_DATA_TOPIC:
				DiozeroProtos.I2C.WriteBlockDataRequest i2c_write_block_data = DiozeroProtos.I2C.WriteBlockDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_block_data)));
				break;
			case MqttProviderConstants.I2C_BLOCK_PROCESS_CALL_TOPIC:
				DiozeroProtos.I2C.BlockProcessCallRequest i2c_block_process_call_data = DiozeroProtos.I2C.BlockProcessCallRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_block_process_call_data)));
				break;
			case MqttProviderConstants.I2C_READ_I2C_BLOCK_DATA_TOPIC:
				DiozeroProtos.I2C.ReadI2CBlockDataRequest i2c_read_i2c_block_data = DiozeroProtos.I2C.ReadI2CBlockDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_read_i2c_block_data)));
				break;
			case MqttProviderConstants.I2C_WRITE_I2C_BLOCK_DATA_TOPIC:
				DiozeroProtos.I2C.WriteI2CBlockDataRequest i2c_write_i2c_block_data = DiozeroProtos.I2C.WriteI2CBlockDataRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_write_i2c_block_data)));
				break;
			case MqttProviderConstants.I2C_CLOSE_TOPIC:
				DiozeroProtos.I2C.CloseRequest i2c_close = DiozeroProtos.I2C.CloseRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(i2c_close)));
				break;

			// SPI
			case MqttProviderConstants.SPI_OPEN_TOPIC:
				DiozeroProtos.Spi.OpenRequest spi_open = DiozeroProtos.Spi.OpenRequest.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_open)));
				break;
			case MqttProviderConstants.SPI_WRITE_TOPIC:
				DiozeroProtos.Spi.WriteRequest spi_write = DiozeroProtos.Spi.WriteRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_write)));
				break;
			case MqttProviderConstants.SPI_WRITE_AND_READ_TOPIC:
				DiozeroProtos.Spi.WriteAndReadRequest spi_write_and_read = DiozeroProtos.Spi.WriteAndReadRequest
						.parseFrom(message.getPayload());
				publishResponse(request(DiozeroProtosConverter.convert(spi_write_and_read)));
				break;
			case MqttProviderConstants.SPI_CLOSE_TOPIC:
				DiozeroProtos.Spi.CloseRequest spi_close = DiozeroProtos.Spi.CloseRequest
						.parseFrom(message.getPayload());
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

	private void publishResponse(GpioGetPwmFrequencyResponse response) throws MqttException {
		DiozeroProtos.Gpio.GetPwmFrequencyResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.GPIO_GET_PWM_FREQUENCY_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(GpioAnalogReadResponse response) throws MqttException {
		DiozeroProtos.Gpio.AnalogReadResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.GPIO_ANALOG_READ_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CBooleanResponse response) throws MqttException {
		DiozeroProtos.I2C.BooleanResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_BOOLEAN_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CByteResponse response) throws MqttException {
		DiozeroProtos.I2C.ByteResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_BYTE_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CBytesResponse response) throws MqttException {
		DiozeroProtos.I2C.BytesResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_BYTES_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CWordResponse response) throws MqttException {
		DiozeroProtos.I2C.WordResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_WORD_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(I2CReadBlockDataResponse response) throws MqttException {
		DiozeroProtos.I2C.ReadBlockDataResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.I2C_READ_BLOCK_DATA_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	private void publishResponse(SpiResponse response) throws MqttException {
		DiozeroProtos.Spi.SpiResponse message = DiozeroProtosConverter.convert(response);

		mqttClient.publish(MqttProviderConstants.SPI_RXDATA_RESPONSE_TOPIC, message.toByteArray(),
				MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
	}

	@Override
	public void accept(DigitalInputEvent event) {
		DiozeroProtos.Gpio.Notification notification = DiozeroProtosConverter.convert(event);

		try {
			mqttClient.publish(MqttProviderConstants.GPIO_NOTIFICATION_TOPIC, notification.toByteArray(),
					MqttProviderConstants.DEFAULT_QOS, MqttProviderConstants.DEFAULT_RETAINED);
		} catch (MqttException e) {
			Logger.error(e, "MQTT Error: {}", e);
		}
	}
}
