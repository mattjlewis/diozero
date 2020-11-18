package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Common
 * Filename:     RemoteProtocolInterface.java  
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

import java.io.Closeable;

public interface RemoteProtocolInterface extends Closeable {
	void start();
	
	@Override
	void close();
	
	GetBoardInfoResponse request(GetBoardInfoRequest request);
	
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
	I2CBooleanResponse request(I2CProbe request);
	Response request(I2CWriteQuick request);
	I2CByteResponse request(I2CReadByte request);
	Response request(I2CWriteByte request);
	I2CBytesResponse request(I2CReadBytes request);
	Response request(I2CWriteBytes request);
	I2CByteResponse request(I2CReadByteData request);
	Response request(I2CWriteByteData request);
	I2CWordResponse request(I2CReadWordData request);
	Response request(I2CWriteWordData request);
	I2CWordResponse request(I2CProcessCall request);
	I2CReadBlockDataResponse request(I2CReadBlockData request);
	Response request(I2CWriteBlockData request);
	I2CBytesResponse request(I2CBlockProcessCall request);
	I2CBytesResponse request(I2CReadI2CBlockData request);
	Response request(I2CWriteI2CBlockData request);
	Response request(I2CClose request);
	
	// SPI
	Response request(SpiOpen request);
	Response request(SpiWrite request);
	SpiResponse request(SpiWriteAndRead request);
	Response request(SpiClose request);
	
	// Serial
	Response request(SerialOpen request);
	SerialReadResponse request(SerialRead request);
	SerialReadByteResponse request(SerialReadByte request);
	Response request(SerialWriteByte request);
	SerialReadBytesResponse request(SerialReadBytes request);
	Response request(SerialWriteBytes request);
	SerialBytesAvailableResponse request(SerialBytesAvailable request);
	Response request(SerialClose request);
}
