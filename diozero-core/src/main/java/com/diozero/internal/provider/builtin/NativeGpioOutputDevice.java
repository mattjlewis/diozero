package com.diozero.internal.provider.builtin;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.util.RuntimeIOException;

public class NativeGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private int gpio;
	private GpioLine line;

	public NativeGpioOutputDevice(DefaultDeviceFactory deviceFactory, String key, GpioChip chip, PinInfo pinInfo,
			boolean initialValue) {
		super(key, deviceFactory);

		gpio = pinInfo.getDeviceNumber();
		int offset = pinInfo.getLineOffset();
		if (offset == PinInfo.NOT_DEFINED) {
			throw new IllegalArgumentException("Line offset not defined for pin " + pinInfo);
		}

		line = chip.provisionGpioOutputDevice(offset, initialValue ? 1 : 0);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return line.getValue() == 0 ? false : true;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		line.setValue(value ? 1 : 0);
	}

	@Override
	public void closeDevice() {
		line.close();
	}
}
