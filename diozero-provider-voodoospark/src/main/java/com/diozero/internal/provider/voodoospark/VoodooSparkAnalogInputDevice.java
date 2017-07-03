package com.diozero.internal.provider.voodoospark;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class VoodooSparkAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent> implements AnalogInputDeviceInterface {
	private VoodooSparkDeviceFactory deviceFactory;
	private int gpio;

	public VoodooSparkAnalogInputDevice(VoodooSparkDeviceFactory deviceFactory, String key,
			PinInfo pinInfo) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		this.gpio = pinInfo.getDeviceNumber();
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return deviceFactory.getAnalogValue(gpio) / (float) VoodooSparkDeviceFactory.MAX_ANALOG_VALUE;
	}

	@Override
	public int getAdcNumber() {
		return gpio;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
	}
}
