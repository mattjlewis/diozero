package com.diozero;

import java.io.IOException;

import com.diozero.api.AnalogueInputDevice;
import com.diozero.api.TemperatureSensorInterface;
import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;

/**
 * TMP36 temperature sensor
 */
public class TMP36 extends AnalogueInputDevice implements TemperatureSensorInterface {
	private double tempOffset;

	public TMP36(AnalogueInputDeviceFactoryInterface deviceFactory, int pinNumber, double tempOffset) throws IOException {
		super(deviceFactory, pinNumber);
		this.tempOffset = tempOffset;
	}

	@Override
	public double getTemperature() throws IOException {
		double v = getValue();
		return (100 * v - 50) + tempOffset;
	}
}
