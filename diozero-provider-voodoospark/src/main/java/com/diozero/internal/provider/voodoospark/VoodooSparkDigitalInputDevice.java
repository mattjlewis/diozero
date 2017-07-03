package com.diozero.internal.provider.voodoospark;

import com.diozero.api.*;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.voodoospark.VoodooSparkDeviceFactory.PinMode;
import com.diozero.util.RuntimeIOException;

public class VoodooSparkDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface {
	private VoodooSparkDeviceFactory deviceFactory;
	private int gpio;

	VoodooSparkDigitalInputDevice(VoodooSparkDeviceFactory deviceFactory, String key,
			PinInfo pinInfo, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();
		
		deviceFactory.setPinMode(gpio, PinMode.DIGITAL_INPUT);
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
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
	}
}
