package com.diozero.remote.server.websocket;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     MessageWrapperTypes.java  
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

public interface MessageWrapperTypes {
	static final String RESPONSE = "Response";
	
	// GPIO Requests
	static final String PROVISION_DIGITAL_INPUT_DEVICE = "ProvisionDigitalInputDevice";
	static final String PROVISION_DIGITAL_OUTPUT_DEVICE = "ProvisionDigitalOutputDevice";
	static final String PROVISION_DIGITAL_INPUT_OUTPUT_DEVICE = "ProvisionDigitalInputOutputDevice";
	static final String PROVISION_PWM_OUTPUT_DEVICE = "ProvisionPwmOutputDevice";
	static final String PROVISION_ANALOG_INPUT_DEVICE = "ProvisionAnalogInputDevice";
	static final String PROVISION_ANALOG_OUTPUT_DEVICE = "ProvisionAnalogOutputDevice";
	static final String GPIO_DIGITAL_READ = "GpioDigitalRead";
	static final String GPIO_DIGITAL_WRITE = "GpioDigitalWrite";
	static final String GPIO_PWM_READ = "GpioPwmRead";
	static final String GPIO_PWM_WRITE = "GpioPwmWrite";
	static final String GPIO_ANALOG_READ = "GpioAnalogRead";
	static final String GPIO_ANALOG_WRITE = "GpioAnalogWrite";
	static final String GPIO_EVENTS = "GpioEvents";
	static final String GPIO_CLOSE = "GpioClose";
	// GPIO Responses
	static final String GPIO_DIGITAL_READ_RESPONSE = "GpioDigitalReadResponse";
	static final String GPIO_PWM_READ_RESPONSE = "GpioPwmReadResponse";
	static final String GPIO_ANALOG_READ_RESPONSE = "GpioAnalogReadResponse";
	static final String DIGITAL_INPUT_EVENT = "DigitalInputEvent";
	
	// I2C Requests
	static final String I2C_OPEN = "I2COpen";
	static final String I2C_READ_BYTE = "I2CReadByte";
	static final String I2C_WRITE_BYTE = "I2CWriteByte";
	static final String I2C_READ = "I2CRead";
	static final String I2C_WRITE = "I2CWrite";
	static final String I2C_READ_BYTE_DATA = "I2CReadByteData";
	static final String I2C_WRITE_BYTE_DATA = "I2CWriteByteData";
	static final String I2C_READ_I2C_BLOCK_DATA = "I2CReadI2CBlockData";
	static final String I2C_WRITE_I2C_BLOCK_DATA = "I2CWriteI2CBlockData";
	static final String I2C_CLOSE = "I2CClose";
	// I2C Responses
	static final String I2C_READ_BYTE_RESPONSE = "I2CReadByteResponse";
	static final String I2C_READ_RESPONSE = "I2CReadResponse";
	
	// SPI Requests
	static final String SPI_OPEN = "SpiOpen";
	static final String SPI_WRITE = "SpiWrite";
	static final String SPI_WRITE_AND_READ = "SpiWriteAndRead";
	static final String SPI_CLOSE = "SpiClose";
	// SPI Responses
	static final String SPI_RESPONSE = "SpiResponse";
}
