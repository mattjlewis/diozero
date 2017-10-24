package com.diozero.remote.message;

import java.io.Closeable;

public interface RemoteProtocolInterface extends Closeable {
	@Override
	void close();
	
	GetBoardGpioInfoResponse request(GetBoardGpioInfo request);
	
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
