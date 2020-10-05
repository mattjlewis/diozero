package com.diozero.remote.mqtt;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Common
 * Filename:     MqttProviderConstants.java  
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

public interface MqttProviderConstants {
	static final int DEFAULT_QOS = 0;
	static final boolean DEFAULT_RETAINED = false;

	static final String ROOT_TOPIC = "diozero";

	static final String REQUEST_TOPIC = ROOT_TOPIC + "/request";
	static final String RESPONSE_TOPIC = ROOT_TOPIC + "/response";
	static final String NOTIFICATION_TOPIC = ROOT_TOPIC + "/notification";
	static final String GET_BOARD_GPIO_INFO_TOPIC = ROOT_TOPIC + "/boardGpioInfo";

	// GPIO
	static final String GPIO_SUB_TOPIC = "/gpio";
	static final String GPIO_REQUEST_TOPIC = REQUEST_TOPIC + GPIO_SUB_TOPIC;
	// GPIO Requests
	static final String GPIO_PROVISION_DIGITAL_INPUT_TOPIC = GPIO_REQUEST_TOPIC + "/in";
	static final String GPIO_PROVISION_DIGITAL_OUTPUT_TOPIC = GPIO_REQUEST_TOPIC + "/out";
	static final String GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_TOPIC = GPIO_REQUEST_TOPIC + "/inout";
	static final String GPIO_PROVISION_PWM_OUTPUT_TOPIC = GPIO_REQUEST_TOPIC + "/pwm";
	static final String GPIO_PROVISION_ANALOG_INPUT_TOPIC = GPIO_REQUEST_TOPIC + "/aIn";
	static final String GPIO_PROVISION_ANALOG_OUTPUT_TOPIC = GPIO_REQUEST_TOPIC + "/aOut";
	static final String GPIO_DIGITAL_READ_TOPIC = GPIO_REQUEST_TOPIC + "/read";
	static final String GPIO_DIGITAL_WRITE_TOPIC = GPIO_REQUEST_TOPIC + "/write";
	static final String GPIO_PWM_READ_TOPIC = GPIO_REQUEST_TOPIC + "/pwmRead";
	static final String GPIO_PWM_WRITE_TOPIC = GPIO_REQUEST_TOPIC + "/pwmWrite";
	static final String GPIO_ANALOG_READ_TOPIC = GPIO_REQUEST_TOPIC + "/analogRead";
	static final String GPIO_ANALOG_WRITE_TOPIC = GPIO_REQUEST_TOPIC + "/analogWrite";
	static final String GPIO_EVENTS_TOPIC = GPIO_REQUEST_TOPIC + "/events";
	static final String GPIO_CLOSE_TOPIC = GPIO_REQUEST_TOPIC + "/close";
	// GPIO Responses
	static final String GPIO_DIGITAL_READ_RESPONSE_TOPIC = RESPONSE_TOPIC + "/gpioDigitalRead";
	static final String GPIO_PWM_READ_RESPONSE_TOPIC = RESPONSE_TOPIC + "/gpioPwmRead";
	static final String GPIO_ANALOG_READ_RESPONSE_TOPIC = RESPONSE_TOPIC + "/gpioAnalogRead";
	static final String GPIO_NOTIFICATION_TOPIC = NOTIFICATION_TOPIC + GPIO_SUB_TOPIC;

	// I2C
	static final String I2C_SUB_TOPIC = "/i2c";
	static final String I2C_REQUEST_TOPIC = REQUEST_TOPIC + I2C_SUB_TOPIC;
	// I2C Requests
	static final String I2C_OPEN_TOPIC = I2C_REQUEST_TOPIC + "/open";
	static final String I2C_READ_BYTE_TOPIC = I2C_REQUEST_TOPIC + "/readByte";
	static final String I2C_WRITE_BYTE_TOPIC = I2C_REQUEST_TOPIC + "/writeByte";
	static final String I2C_READ_TOPIC = I2C_REQUEST_TOPIC + "/read";
	static final String I2C_WRITE_TOPIC = I2C_REQUEST_TOPIC + "/write";
	static final String I2C_READ_BYTE_DATA_TOPIC = I2C_REQUEST_TOPIC + "/readByteData";
	static final String I2C_WRITE_BYTE_DATA_TOPIC = I2C_REQUEST_TOPIC + "/writeByteData";
	static final String I2C_READ_I2C_BLOCK_DATA_TOPIC = I2C_REQUEST_TOPIC + "/readI2CBlockData";
	static final String I2C_WRITE_I2C_BLOCK_DATA_TOPIC = I2C_REQUEST_TOPIC + "/writeI2CBlockData";
	static final String I2C_CLOSE_TOPIC = I2C_REQUEST_TOPIC + "/close";
	// I2C Responses
	static final String I2C_READ_BYTE_RESPONSE_TOPIC = RESPONSE_TOPIC + "/i2cReadByte";
	static final String I2C_READ_RESPONSE_TOPIC = RESPONSE_TOPIC + "/i2cRead";

	// SPI
	static final String SPI_SUB_TOPIC = "/spi";
	static final String SPI_REQUEST_TOPIC = REQUEST_TOPIC + SPI_SUB_TOPIC;
	// SPI Requests
	static final String SPI_OPEN_TOPIC = SPI_REQUEST_TOPIC + "/open";
	static final String SPI_WRITE_TOPIC = SPI_REQUEST_TOPIC + "/write";
	static final String SPI_WRITE_AND_READ_TOPIC = SPI_REQUEST_TOPIC + "/writeAndRead";
	static final String SPI_CLOSE_TOPIC = SPI_REQUEST_TOPIC + "/close";
	// SPI Responses
	static final String SPI_RXDATA_RESPONSE_TOPIC = RESPONSE_TOPIC + "/spiRxData";

	// Serial
	static final String SERIAL_SUB_TOPIC = "/serial";
	static final String SERIAL_REQUEST_TOPIC = REQUEST_TOPIC + SERIAL_SUB_TOPIC;
	// Serial Requests
	static final String SERIAL_OPEN_TOPIC = SERIAL_REQUEST_TOPIC + "/open";
	static final String SERIAL_READ_TOPIC = SPI_REQUEST_TOPIC + "/read";
	static final String SERIAL_READ_BYTE_TOPIC = SPI_REQUEST_TOPIC + "/readByte";
	static final String SERIAL_WRITE_BYTE_TOPIC = SPI_REQUEST_TOPIC + "/writeByte";
	static final String SERIAL_READ_BYTES_TOPIC = SPI_REQUEST_TOPIC + "/readBytes";
	static final String SERIAL_WRITE_BYTES_TOPIC = SPI_REQUEST_TOPIC + "/writeBytes";
	static final String SERIAL_BYTES_AVAILABLE_TOPIC = SPI_REQUEST_TOPIC + "/bytesAvailable";
	static final String SERIAL_CLOSE_TOPIC = SPI_REQUEST_TOPIC + "/close";
	// Serial Responses
	static final String SERIAL_READ_RESPONSE_TOPIC = RESPONSE_TOPIC + "/serialReadResponse";
	static final String SERIAL_READ_BYTE_RESPONSE_TOPIC = RESPONSE_TOPIC + "/serialReadByte";
	static final String SERIAL_READ_BYTES_RESPONSE_TOPIC = RESPONSE_TOPIC + "/serialReadBytesResponse";
}
