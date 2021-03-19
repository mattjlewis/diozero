package com.diozero.remote.http;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Common
 * Filename:     HttpProviderConstants.java  
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

public interface HttpProviderConstants {
	static final int DEFAULT_QOS = 0;
	static final boolean DEFAULT_RETAINED = false;

	static final String ROOT_URL = "/diozero";

	static final String REQUEST_URL = ROOT_URL + "/request";
	static final String RESPONSE_URL = ROOT_URL + "/response";
	static final String NOTIFICATION_URL = ROOT_URL + "/notification";
	
	static final String GET_BOARD_GPIO_INFO = ROOT_URL + "/boardGpioInfo";

	// GPIO
	static final String GPIO_SUB_URL = "/gpio";
	static final String GPIO_REQUEST_URL = REQUEST_URL + GPIO_SUB_URL;
	// GPIO Requests
	static final String GPIO_PROVISION_DIGITAL_INPUT_URL = GPIO_REQUEST_URL + "/in";
	static final String GPIO_PROVISION_DIGITAL_OUTPUT_URL = GPIO_REQUEST_URL + "/out";
	static final String GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_URL = GPIO_REQUEST_URL + "/inout";
	static final String GPIO_PROVISION_PWM_OUTPUT_URL = GPIO_REQUEST_URL + "/pwm";
	static final String GPIO_PROVISION_ANALOG_INPUT_URL = GPIO_REQUEST_URL + "/aIn";
	static final String GPIO_PROVISION_ANALOG_OUTPUT_URL = GPIO_REQUEST_URL + "/aOut";
	static final String GPIO_DIGITAL_READ_URL = GPIO_REQUEST_URL + "/digitalRead";
	static final String GPIO_DIGITAL_WRITE_URL = GPIO_REQUEST_URL + "/digitalWrite";
	static final String GPIO_PWM_READ_URL = GPIO_REQUEST_URL + "/pwmRead";
	static final String GPIO_PWM_WRITE_URL = GPIO_REQUEST_URL + "/pwmWrite";
	static final String GPIO_GET_PWM_FREQUENCY_URL = GPIO_REQUEST_URL + "/getPwmFrequency";
	static final String GPIO_SET_PWM_FREQUENCY_URL = GPIO_REQUEST_URL + "/setPwmFrequency";
	static final String GPIO_ANALOG_READ_URL = GPIO_REQUEST_URL + "/analogRead";
	static final String GPIO_ANALOG_WRITE_URL = GPIO_REQUEST_URL + "/analogWrite";
	static final String GPIO_EVENTS_URL = GPIO_REQUEST_URL + "/events";
	static final String GPIO_CLOSE_URL = GPIO_REQUEST_URL + "/close";
	// GPIO Responses
	static final String GPIO_RESPONSE_URL = RESPONSE_URL + GPIO_SUB_URL;
	static final String GPIO_NOTIFICATION_URL = NOTIFICATION_URL + GPIO_SUB_URL;

	// I2C
	static final String I2C_SUB_URL = "/i2c";
	static final String I2C_REQUEST_URL = REQUEST_URL + I2C_SUB_URL;
	// I2C Requests
	static final String I2C_OPEN_URL = I2C_REQUEST_URL + "/open";
	static final String I2C_PROBE_URL = I2C_REQUEST_URL + "/probe";
	static final String I2C_WRITE_QUICK_URL = I2C_REQUEST_URL + "/writeQuick";
	static final String I2C_READ_BYTE_URL = I2C_REQUEST_URL + "/readByte";
	static final String I2C_WRITE_BYTE_URL = I2C_REQUEST_URL + "/writeByte";
	static final String I2C_READ_BYTES_URL = I2C_REQUEST_URL + "/read";
	static final String I2C_WRITE_BYTES_URL = I2C_REQUEST_URL + "/write";
	static final String I2C_READ_BYTE_DATA_URL = I2C_REQUEST_URL + "/readByteData";
	static final String I2C_WRITE_BYTE_DATA_URL = I2C_REQUEST_URL + "/writeByteData";
	static final String I2C_READ_WORD_DATA_URL = I2C_REQUEST_URL + "/readWordData";
	static final String I2C_WRITE_WORD_DATA_URL = I2C_REQUEST_URL + "/writeWordData";
	static final String I2C_PROCESS_CALL_URL = I2C_REQUEST_URL + "/processCall";
	static final String I2C_READ_BLOCK_DATA_URL = I2C_REQUEST_URL + "/readBlockData";
	static final String I2C_WRITE_BLOCK_DATA_URL = I2C_REQUEST_URL + "/writeBlockData";
	static final String I2C_BLOCK_PROCESS_CALL_URL = I2C_REQUEST_URL + "/blockProcessCall";
	static final String I2C_READ_I2C_BLOCK_DATA_URL = I2C_REQUEST_URL + "/readI2CBlockData";
	static final String I2C_WRITE_I2C_BLOCK_DATA_URL = I2C_REQUEST_URL + "/writeI2CBlockData";
	static final String I2C_CLOSE_URL = I2C_REQUEST_URL + "/close";
	// I2C Responses
	static final String I2C_READ_BYTE_RESPONSE_URL = RESPONSE_URL + "/i2cReadByte";
	static final String I2C_READ_RESPONSE_URL = RESPONSE_URL + "/i2cRead";

	// SPI
	static final String SPI_SUB_URL = "/spi";
	static final String SPI_REQUEST_URL = REQUEST_URL + SPI_SUB_URL;
	// SPI Requests
	static final String SPI_OPEN_URL = SPI_REQUEST_URL + "/open";
	static final String SPI_WRITE_URL = SPI_REQUEST_URL + "/write";
	static final String SPI_WRITE_AND_READ_URL = SPI_REQUEST_URL + "/writeAndRead";
	static final String SPI_CLOSE_URL = SPI_REQUEST_URL + "/close";
	// SPI Responses
	static final String SPI_RXDATA_RESPONSE_URL = RESPONSE_URL + "/spiRxData";

	// Serial
	static final String SERIAL_SUB_URL = "/serial";
	static final String SERIAL_REQUEST_URL = REQUEST_URL + SERIAL_SUB_URL;
	// Serial Requests
	static final String SERIAL_OPEN_URL = SERIAL_REQUEST_URL + "/open";
	static final String SERIAL_READ_URL = SERIAL_REQUEST_URL + "/read";
	static final String SERIAL_READ_BYTE_URL = SERIAL_REQUEST_URL + "/readByte";
	static final String SERIAL_WRITE_BYTE_URL = SERIAL_REQUEST_URL + "/writeByte";
	static final String SERIAL_READ_BYTES_URL = SERIAL_REQUEST_URL + "/readBytes";
	static final String SERIAL_WRITE_BYTES_URL = SERIAL_REQUEST_URL + "/writeBytes";
	static final String SERIAL_BYTES_AVAILABLE_URL = SERIAL_REQUEST_URL + "/bytesAvailable";
	static final String SERIAL_CLOSE_URL = SERIAL_REQUEST_URL + "/close";
	// Serial Responses
	static final String SERIAL_READ_RESPONSE_URL = RESPONSE_URL + "/serialRead";
	static final String SERIAL_READ_BYTE_RESPONSE_URL = RESPONSE_URL + "/serialReadByte";
	static final String SERIAL_READ_BYTES_RESPONSE_URL = RESPONSE_URL + "/serialReadBytes";
	static final String SERIAL_BYTES_AVAILABLE_RESPONSE_URL = RESPONSE_URL + "/serialBytesAvailable";
}
