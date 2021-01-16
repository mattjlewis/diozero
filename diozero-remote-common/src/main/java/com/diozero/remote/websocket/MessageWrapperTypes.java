package com.diozero.remote.websocket;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Common
 * Filename:     MessageWrapperTypes.java  
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
	static final String I2C_PROBE = "I2CProbe";
	static final String I2C_WRITE_QUICK = "I2CWriteQuick";
	static final String I2C_READ_BYTE = "I2CReadByte";
	static final String I2C_WRITE_BYTE = "I2CWriteByte";
	static final String I2C_READ_BYTES = "I2CReadBytes";
	static final String I2C_WRITE_BYTES = "I2CWriteBytes";
	static final String I2C_READ_BYTE_DATA = "I2CReadByteData";
	static final String I2C_WRITE_BYTE_DATA = "I2CWriteByteData";
	static final String I2C_READ_WORD_DATA = "I2CReadWordData";
	static final String I2C_WRITE_WORD_DATA = "I2CWriteWordData";
	static final String I2C_PROCESS_CALL = "I2CProcessCall";
	static final String I2C_READ_BLOCK_DATA = "I2CReadBlockData";
	static final String I2C_WRITE_BLOCK_DATA = "I2CWriteBlockData";
	static final String I2C_BLOCK_PROCESS_CALL = "I2CBlockProcessCall";
	static final String I2C_READ_I2C_BLOCK_DATA = "I2CReadI2CBlockData";
	static final String I2C_WRITE_I2C_BLOCK_DATA = "I2CWriteI2CBlockData";
	static final String I2C_CLOSE = "I2CClose";
	// I2C Responses
	static final String I2C_BOOLEAN_RESPONSE = "I2CBooleanResponse";
	static final String I2C_BYTE_RESPONSE = "I2CByteResponse";
	static final String I2C_BYTES_RESPONSE = "I2CBytesResponse";
	static final String I2C_WORD_RESPONSE = "I2CWordsResponse";
	static final String I2C_READ_BLOCK_DATA_RESPONSE = "I2CReadBlockDataResponse";
	
	// SPI Requests
	static final String SPI_OPEN = "SpiOpen";
	static final String SPI_WRITE = "SpiWrite";
	static final String SPI_WRITE_AND_READ = "SpiWriteAndRead";
	static final String SPI_CLOSE = "SpiClose";
	// SPI Responses
	static final String SPI_RESPONSE = "SpiResponse";
	
	// Serial Requests
	static final String SERIAL_OPEN = "SerialOpen";
	static final String SERIAL_READ = "SerialRead";
	static final String SERIAL_READ_BYTE = "SerialReadByte";
	static final String SERIAL_WRITE_BYTE = "SerialWriteByte";
	static final String SERIAL_READ_BYTES = "SerialReadBytes";
	static final String SERIAL_WRITE_BYTES = "SerialWriteBytes";
	static final String SERIAL_CLOSE = "SerialClose";
	// Serial Responses
	static final String SERIAL_READ_RESPONSE = "SerialReadResponse";
	static final String SERIAL_READ_BYTE_RESPONSE = "SerialReadByteResponse";
	static final String SERIAL_READ_BYTES_RESPONSE = "SerialReadBytesResponse";
}
