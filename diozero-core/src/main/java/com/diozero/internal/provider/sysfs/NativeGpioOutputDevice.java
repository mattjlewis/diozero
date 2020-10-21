package com.diozero.internal.provider.sysfs;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class NativeGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private NativeGpioChip chip;
	private int gpio;
	private int offset;
	private GpioLine line;
	
	public NativeGpioOutputDevice(DefaultDeviceFactory deviceFactory, String key, NativeGpioChip chip,
			PinInfo pinInfo, boolean initialValue) {
		super(key, deviceFactory);
		
		gpio = pinInfo.getDeviceNumber();
		offset = chip.getOffset(gpio);
		if (offset == -1) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + " - offset not found");
		}
		this.chip = chip;

		line = chip.provisionGpioOutputDevice(gpio, initialValue ? 1 : 0);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return chip.getValue(offset) == 0 ? false : true;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		chip.setValue(offset, value ? 1 : 0);
	}
	
	@Override
	public void closeDevice() {
		NativeGpioChip.close(line.getFd());
	}
}
