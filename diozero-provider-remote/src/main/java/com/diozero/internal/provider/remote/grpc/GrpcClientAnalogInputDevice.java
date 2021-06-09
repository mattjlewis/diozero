package com.diozero.internal.provider.remote.grpc;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceInterface;

public class GrpcClientAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent>
		implements AnalogInputDeviceInterface {
	private GrpcClientDeviceFactory deviceFactory;
	private int gpio;

	public GrpcClientAnalogInputDevice(GrpcClientDeviceFactory deviceFactory, String key, PinInfo pinInfo) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();

		deviceFactory.provisionAnalogInputDevice(gpio);
	}

	@Override
	public boolean generatesEvents() {
		return true;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return deviceFactory.analogRead(gpio);
	}

	@Override
	public int getAdcNumber() {
		return gpio;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		deviceFactory.closeGpio(gpio);
	}
}
