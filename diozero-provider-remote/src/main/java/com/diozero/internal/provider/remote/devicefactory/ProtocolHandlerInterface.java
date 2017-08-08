package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     ProtocolHandlerInterface.java  
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

import java.io.Closeable;

import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionSpiDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;

public interface ProtocolHandlerInterface extends Closeable {
	@Override
	void close();

	Response sendRequest(ProvisionDigitalInputDevice request);
	Response sendRequest(ProvisionDigitalOutputDevice request);
	Response sendRequest(ProvisionDigitalInputOutputDevice request);
	GpioDigitalReadResponse sendRequest(GpioDigitalRead request);
	Response sendRequest(GpioDigitalWrite request);
	Response sendRequest(GpioEvents request);
	Response sendRequest(GpioClose request);

	Response sendRequest(ProvisionSpiDevice request);
	Response sendRequest(SpiWrite request);
	SpiResponse sendRequest(SpiWriteAndRead request);
	Response sendRequest(SpiClose request);
}
