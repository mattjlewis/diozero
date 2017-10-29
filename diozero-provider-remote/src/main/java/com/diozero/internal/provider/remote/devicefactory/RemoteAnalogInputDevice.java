package com.diozero.internal.provider.remote.devicefactory;

import java.util.UUID;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.remote.message.ProvisionAnalogInputDevice;
import com.diozero.remote.message.Response;
import com.diozero.util.RuntimeIOException;

public class RemoteAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent> implements AnalogInputDeviceInterface {
	private RemoteDeviceFactory deviceFactory;
	private int gpio;

	public RemoteAnalogInputDevice(RemoteDeviceFactory deviceFactory, String key, PinInfo pinInfo) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();
		
		ProvisionAnalogInputDevice request = new ProvisionAnalogInputDevice(gpio, UUID.randomUUID().toString());
		Response response = deviceFactory.getProtocolHandler().request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error: " + response.getDetail());
		}
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
