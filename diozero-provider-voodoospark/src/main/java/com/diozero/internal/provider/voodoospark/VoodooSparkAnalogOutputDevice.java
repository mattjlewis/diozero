package com.diozero.internal.provider.voodoospark;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class VoodooSparkAnalogOutputDevice extends AbstractDevice implements AnalogOutputDeviceInterface {
	private VoodooSparkDeviceFactory deviceFactory;
	private int gpio;
	
	public VoodooSparkAnalogOutputDevice(VoodooSparkDeviceFactory deviceFactory, String key, PinInfo pinInfo) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		this.gpio = pinInfo.getDeviceNumber();
	}

	@Override
	public int getAdcNumber() {
		return gpio;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return deviceFactory.getAnalogValue(gpio) / (float) VoodooSparkDeviceFactory.MAX_ANALOG_VALUE;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		deviceFactory.setAnalogValue(gpio, (int) (value * VoodooSparkDeviceFactory.MAX_ANALOG_VALUE));
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
	}
}
