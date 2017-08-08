package com.diozero.remote.websocket;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     MessageWrapperTypes.java  
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

public interface MessageWrapperTypes {
	static final String PROVISION_DIGITAL_INPUT_DEVICE = "ProvisionDigitalInputDevice";
	static final String PROVISION_DIGITAL_OUTPUT_DEVICE = "ProvisionDigitalOutputDevice";
	static final String PROVISION_DIGITAL_INPUT_OUTPUT_DEVICE = "ProvisionDigitalInputOutputDevice";
	static final String GPIO_DIGITAL_READ = "GpioDigitalRead";
	static final String GPIO_DIGITAL_WRITE = "GpioDigitalWrite";
	static final String GPIO_EVENTS = "GpioEvents";
	static final String GPIO_CLOSE = "GpioClose";
	
	static final String PROVISION_SPI_DEVICE = "ProvisionSpiDevice";
	static final String SPI_WRITE = "SpiWrite";
	static final String SPI_WRITE_AND_READ = "SpiWriteAndRead";
	static final String SPI_CLOSE = "SpiClose";
	
	static final String RESPONSE = "Response";
	static final String GPIO_DIGITAL_READ_RESPONSE = "GpioDigitalReadResponse";
	static final String SPI_RESPONSE = "SpiResponse";
	
	static final String DIGITAL_INPUT_EVENT = "DigitalInputEvent";
}
