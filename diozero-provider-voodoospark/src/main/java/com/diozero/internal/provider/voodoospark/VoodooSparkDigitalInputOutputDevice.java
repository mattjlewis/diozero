package com.diozero.internal.provider.voodoospark;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.voodoospark.VoodooSparkDeviceFactory.PinMode;
import com.diozero.util.RuntimeIOException;

public class VoodooSparkDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputOutputDeviceInterface {
	private int gpio;
	private DeviceMode mode;
	private VoodooSparkDeviceFactory deviceFactory;

	public VoodooSparkDigitalInputOutputDevice(VoodooSparkDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			DeviceMode mode) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();
		
		setMode(mode);
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
	public int getGpio() {
		return gpio;
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		deviceFactory.setPinMode(gpio, mode == DeviceMode.DIGITAL_INPUT ? PinMode.DIGITAL_INPUT : PinMode.DIGITAL_OUTPUT);
		this.mode = mode;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
	}
}
