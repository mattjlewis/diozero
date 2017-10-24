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
import com.diozero.remote.message.GetBoardGpioInfo;
import com.diozero.remote.message.GetBoardGpioInfoResponse;
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
import com.diozero.remote.server.mqtt.MqttProviderConstants;
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
	public GetBoardGpioInfoResponse request(GetBoardGpioInfo request) {
		return (GetBoardGpioInfoResponse) requestResponse(MqttProviderConstants.GET_BOARD_GPIO_INFO_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_PWM_OUTPUT_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_ANALOG_INPUT_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		return requestResponse(MqttProviderConstants.GPIO_PROVISION_ANALOG_OUTPUT_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		return (GpioDigitalReadResponse) requestResponse(MqttProviderConstants.GPIO_DIGITAL_READ_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		return requestResponse(MqttProviderConstants.GPIO_DIGITAL_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		return (GpioPwmReadResponse) requestResponse(MqttProviderConstants.GPIO_PWM_READ_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioPwmWrite request) {
		return requestResponse(MqttProviderConstants.GPIO_PWM_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		return (GpioAnalogReadResponse) requestResponse(MqttProviderConstants.GPIO_ANALOG_READ_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		return requestResponse(MqttProviderConstants.GPIO_ANALOG_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioEvents request) {
		return requestResponse(MqttProviderConstants.GPIO_EVENTS_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(GpioClose request) {
		return requestResponse(MqttProviderConstants.GPIO_CLOSE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(I2COpen request) {
		return requestResponse(MqttProviderConstants.I2C_OPEN_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public I2CReadByteResponse request(I2CReadByte request) {
		return (I2CReadByteResponse) requestResponse(MqttProviderConstants.I2C_READ_BYTE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteByte request) {
		return requestResponse(MqttProviderConstants.I2C_WRITE_BYTE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public I2CReadResponse request(I2CRead request) {
		return (I2CReadResponse) requestResponse(MqttProviderConstants.I2C_READ_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public Response request(I2CWrite request) {
		return requestResponse(MqttProviderConstants.I2C_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public I2CReadByteResponse request(I2CReadByteData request) {
		return (I2CReadByteResponse) requestResponse(MqttProviderConstants.I2C_READ_BYTE_DATA_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public Response request(I2CWriteByteData request) {
		return requestResponse(MqttProviderConstants.I2C_WRITE_BYTE_DATA_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public I2CReadResponse request(I2CReadI2CBlockData request) {
		return (I2CReadResponse) requestResponse(MqttProviderConstants.I2C_READ_I2C_BLOCK_DATA_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public Response request(I2CWriteI2CBlockData request) {
		return requestResponse(MqttProviderConstants.I2C_WRITE_I2C_BLOCK_DATA_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}
	
	@Override
	public Response request(I2CClose request) {
		return requestResponse(MqttProviderConstants.I2C_CLOSE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SpiOpen request) {
		return requestResponse(MqttProviderConstants.SPI_OPEN_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SpiWrite request) {
		return requestResponse(MqttProviderConstants.SPI_WRITE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		return (SpiResponse) requestResponse(MqttProviderConstants.SPI_WRITE_AND_READ_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
	}

	@Override
	public Response request(SpiClose request) {
		return requestResponse(MqttProviderConstants.SPI_CLOSE_TOPIC,
				DiozeroProtosConverter.convert(request), request.getCorrelationId());
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
				processResponse(DiozeroProtosConverter.convert(response));
				break;
			case MqttProviderConstants.GPIO_DIGITAL_READ_RESPONSE_TOPIC:
				DiozeroProtos.Gpio.DigitalReadResponse digital_read_response = DiozeroProtos.Gpio.DigitalReadResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(digital_read_response));
				break;
			case MqttProviderConstants.GPIO_PWM_READ_RESPONSE_TOPIC:
				DiozeroProtos.Gpio.PwmReadResponse pwm_read_response = DiozeroProtos.Gpio.PwmReadResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(pwm_read_response));
				break;
			case MqttProviderConstants.GPIO_ANALOG_READ_RESPONSE_TOPIC:
				DiozeroProtos.Gpio.AnalogReadResponse analog_read_response = DiozeroProtos.Gpio.AnalogReadResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(analog_read_response));
				break;
			case MqttProviderConstants.GPIO_NOTIFICATION_TOPIC:
				processEvent(DiozeroProtosConverter
						.convert(DiozeroProtos.Gpio.Notification.parseFrom(message.getPayload())));
				break;
			case MqttProviderConstants.I2C_READ_BYTE_RESPONSE_TOPIC:
				DiozeroProtos.I2C.ReadByteResponse i2c_read_byte_response = DiozeroProtos.I2C.ReadByteResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(i2c_read_byte_response));
				break;
			case MqttProviderConstants.I2C_READ_RESPONSE_TOPIC:
				DiozeroProtos.I2C.ReadResponse i2c_read_response = DiozeroProtos.I2C.ReadResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(i2c_read_response));
				break;
			case MqttProviderConstants.SPI_RXDATA_RESPONSE_TOPIC:
				DiozeroProtos.Spi.SpiResponse spi_response = DiozeroProtos.Spi.SpiResponse
						.parseFrom(message.getPayload());
				processResponse(DiozeroProtosConverter.convert(spi_response));
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
