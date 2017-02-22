package com.diozero.internal.provider.firmata;

import java.io.IOException;

import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;

import com.diozero.api.AnalogInputEvent;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class FirmataAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent> implements AnalogInputDeviceInterface {
	private static final float RANGE = 1023;
	
	private Pin pin;

	public FirmataAnalogInputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		try {
			pin.setMode(Mode.ANALOG);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting pin mode to analog input for pin " + deviceNumber);
		}
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return pin.getValue() / RANGE;
	}

	@Override
	public int getAdcNumber() {
		return pin.getIndex();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		// TODO Anything to do?
	}
}
