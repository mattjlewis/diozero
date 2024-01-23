package com.diozero.internal.provider.firmata.example;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataTestConstants.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

public interface FirmataTestConstants {
	String SERIAL_PORT_PROP = "diozero.firmata.serialPort";
	String SERIAL_BAUD_PROP = "diozero.firmata.serialBaud";
	String SERIAL_DATA_BITS_PROP = "diozero.firmata.serialDataBits";
	String SERIAL_STOP_BITS_PROP = "diozero.firmata.serialStopBits";
	String SERIAL_PARITY_PROP = "diozero.firmata.serialParity";
	String TCP_HOST_PROP = "diozero.firmata.tcpHostname";
	String TCP_PORT_PROP = "diozero.firmata.tcpPort";
	int DEFAULT_TCP_PORT = 3030;
}
