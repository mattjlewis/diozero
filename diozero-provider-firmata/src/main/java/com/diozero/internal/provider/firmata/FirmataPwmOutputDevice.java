package com.diozero.internal.provider.firmata;

import java.io.IOException;

import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class FirmataPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final float PWM_MAX = 255;
	
	private Pin pin;
	
	public FirmataPwmOutputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber,
			float initialValue) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		try {
			pin.setMode(Mode.PWM);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting pin mode to PWM for pin " + deviceNumber);
		}
		setValue(initialValue);
	}

	@Override
	public int getGpio() {
		return pin.getIndex();
	}

	@Override
	public int getPwmNum() {
		return pin.getIndex();
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return pin.getValue() / PWM_MAX;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		try {
			pin.setValue((int) (value * PWM_MAX));
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting PWM value to " + value + " for pin " + pin.getIndex());
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		setValue(0);
		// TODO Anything else to do?
	}
}
