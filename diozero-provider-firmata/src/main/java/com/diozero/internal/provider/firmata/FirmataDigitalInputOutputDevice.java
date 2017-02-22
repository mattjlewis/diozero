package com.diozero.internal.provider.firmata;

import java.io.IOException;

import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;

import com.diozero.api.DeviceMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class FirmataDigitalInputOutputDevice extends AbstractDevice implements GpioDigitalInputOutputDeviceInterface {
	private Pin pin;
	private DeviceMode mode;

	public FirmataDigitalInputOutputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber,
			DeviceMode mode) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		
		setMode(mode);
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
	public boolean getValue() throws RuntimeIOException {
		return pin.getValue() != 0;
	}

	@Override
	public int getGpio() {
		return pin.getIndex();
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		Mode firmata_mode;
		switch (mode) {
		case DIGITAL_INPUT:
			firmata_mode = Mode.INPUT;
			break;
		case DIGITAL_OUTPUT:
			firmata_mode = Mode.OUTPUT;
			break;
		default:
			throw new IllegalArgumentException("Invalid mode " + mode);
		}
		try {
			pin.setMode(firmata_mode);
			this.mode = mode;
		} catch (IllegalArgumentException | IOException e) {
			throw new RuntimeIOException("Error setting mode to " + mode + " for pin " + pin.getIndex());
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		if (mode == DeviceMode.DIGITAL_OUTPUT) {
			setValue(false);
		}
		// TODO Nothing else to do?
	}
}
