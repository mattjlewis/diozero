package com.diozero.internal.provider.voodoospark;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.voodoospark.VoodooSparkDeviceFactory.PinMode;
import com.diozero.util.RuntimeIOException;

public class VoodooSparkPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private VoodooSparkDeviceFactory deviceFactory;
	private int gpio;

	public VoodooSparkPwmOutputDevice(VoodooSparkDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			int pwmFrequency, float initialValue) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();
		
		deviceFactory.setPinMode(gpio, PinMode.ANALOG_OUTPUT);
		setValue(initialValue);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getPwmNum() {
		return gpio;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return deviceFactory.getAnalogValue(gpio) / (float) VoodooSparkDeviceFactory.MAX_PWM_VALUE;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		deviceFactory.setAnalogValue(gpio, (int) (value * VoodooSparkDeviceFactory.MAX_PWM_VALUE));
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		setValue(0);
	}
}
