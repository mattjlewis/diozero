package com.diozero.remote.http;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     HttpProviderConstants.java  
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

public interface HttpProviderConstants {
	static final int DEFAULT_QOS = 0;
	static final boolean DEFAULT_RETAINED = false;

	static final String ROOT_URL = "/diozero";

	static final String REQUEST_URL = ROOT_URL + "/request";
	static final String RESPONSE_URL = ROOT_URL + "/response";
	static final String NOTIFICATION_URL = ROOT_URL + "/notification";

	static final String GPIO_SUB_URL = "/gpio";
	static final String GPIO_REQUEST_URL = REQUEST_URL + GPIO_SUB_URL;
	static final String GPIO_RESPONSE_URL = RESPONSE_URL + GPIO_SUB_URL;
	static final String GPIO_NOTIFICATION_URL = NOTIFICATION_URL + GPIO_SUB_URL;

	static final String GPIO_PROVISION_INPUT_URL = GPIO_REQUEST_URL + "/in";
	static final String GPIO_PROVISION_OUTPUT_URL = GPIO_REQUEST_URL + "/out";
	static final String GPIO_PROVISION_INPUT_OUTPUT_URL = GPIO_REQUEST_URL + "/inout";
	static final String GPIO_READ_URL = GPIO_REQUEST_URL + "/read";
	static final String GPIO_WRITE_URL = GPIO_REQUEST_URL + "/write";
	static final String GPIO_EVENTS_URL = GPIO_REQUEST_URL + "/events";
	static final String GPIO_CLOSE_URL = GPIO_REQUEST_URL + "/close";

	static final String SPI_SUB_URL = "/spi";
	static final String SPI_REQUEST_URL = REQUEST_URL + SPI_SUB_URL;
	static final String SPI_RESPONSE_URL = RESPONSE_URL + SPI_SUB_URL;

	static final String SPI_PROVISION_URL = SPI_REQUEST_URL + "/open";
	static final String SPI_WRITE_URL = SPI_REQUEST_URL + "/write";
	static final String SPI_WRITE_AND_READ_URL = SPI_REQUEST_URL + "/writeAndRead";
	static final String SPI_CLOSE_URL = SPI_REQUEST_URL + "/close";

	static final String I2C_SUB_URL = "/i2c";
	static final String I2C_REQUEST_URL = REQUEST_URL + I2C_SUB_URL;
	static final String I2C_RESPONSE_URL = RESPONSE_URL + I2C_SUB_URL;

	static final String I2C_OPEN_URL = I2C_REQUEST_URL + "/open";
	// TODO Full set of I2C URLs...
	static final String I2C_READ_URL = I2C_REQUEST_URL + "/read";
	static final String I2C_WRITE_URL = I2C_REQUEST_URL + "/write";
	static final String I2C_CLOSE_URL = I2C_REQUEST_URL + "/close";
}
