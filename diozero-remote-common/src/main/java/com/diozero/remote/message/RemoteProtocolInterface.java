package com.diozero.remote.message;

public interface RemoteProtocolInterface extends AutoCloseable {
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
