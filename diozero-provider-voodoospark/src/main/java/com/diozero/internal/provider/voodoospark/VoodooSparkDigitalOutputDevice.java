package com.diozero.internal.provider.voodoospark;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.voodoospark.VoodooSparkDeviceFactory.PinMode;
import com.diozero.util.RuntimeIOException;

public class VoodooSparkDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private VoodooSparkDeviceFactory deviceFactory;
	private int gpio;
	
	VoodooSparkDigitalOutputDevice(VoodooSparkDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			boolean initialValue) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();
		
		deviceFactory.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
		deviceFactory.setValue(gpio, initialValue);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return deviceFactory.getValue(gpio);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		deviceFactory.setValue(gpio, value);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
	}
}
