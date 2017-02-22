package com.diozero.internal.provider.firmata;

import java.io.IOException;

import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class FirmataDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private Pin pin;

	public FirmataDigitalOutputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber,
			boolean initialValue) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		try {
			pin.setMode(Mode.OUTPUT);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting pin mode to output for pin " + deviceNumber);
		}
		setValue(initialValue);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return pin.getValue() != 0;
	}

	@Override
	public int getGpio() {
		return pin.getIndex();
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		try {
			pin.setValue(value ? 1 : 0);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting output value for pin " + pin.getIndex());
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		setValue(false);
	}
}
