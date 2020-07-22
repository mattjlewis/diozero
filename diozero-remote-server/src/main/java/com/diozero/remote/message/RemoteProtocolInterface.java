package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     RemoteProtocolInterface.java  
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

import java.io.Closeable;

public interface RemoteProtocolInterface extends Closeable {
	@Override
	void close();
	
	GetBoardInfoResponse request(GetBoardInfo request);
	
	// GPIO
	Response request(ProvisionDigitalInputDevice request);
	Response request(ProvisionDigitalOutputDevice request);
	Response request(ProvisionDigitalInputOutputDevice request);
	Response request(ProvisionPwmOutputDevice request);
	Response request(ProvisionAnalogInputDevice request);
	Response request(ProvisionAnalogOutputDevice request);
	GpioDigitalReadResponse request(GpioDigitalRead request);
	Response request(GpioDigitalWrite request);
	GpioPwmReadResponse request(GpioPwmRead request);
	Response request(GpioPwmWrite request);
	GpioAnalogReadResponse request(GpioAnalogRead request);
	Response request(GpioAnalogWrite request);
	Response request(GpioEvents request);
	Response request(GpioClose request);
	
	// I2C
	Response request(I2COpen request);
	I2CReadByteResponse request(I2CReadByte request);
	Response request(I2CWriteByte request);
	I2CReadResponse request(I2CRead request);
	Response request(I2CWrite request);
	I2CReadByteResponse request(I2CReadByteData request);
	Response request(I2CWriteByteData request);
	I2CReadResponse request(I2CReadI2CBlockData request);
	Response request(I2CWriteI2CBlockData request);
	Response request(I2CClose request);
	
	// SPI
	Response request(SpiOpen request);
	Response request(SpiWrite request);
	SpiResponse request(SpiWriteAndRead request);
	Response request(SpiClose request);
}
