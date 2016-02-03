package com.diozero.internal.spi;

public interface NativeDeviceFactoryInterface extends GpioDeviceFactoryInterface, SpiDeviceFactoryInterface,
		I2CDeviceFactoryInterface, PwmOutputDeviceFactoryInterface, AnalogueInputDeviceFactoryInterface {
}
