package com.diozero.internal.provider.mock;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;

public class MockDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface {
	private final MockDeviceFactory deviceFactory;
	private final MockGpio mock;

	public MockDigitalInputOutputDevice(String key, MockDeviceFactory deviceFactory, int gpio, DeviceMode mode) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		mock = deviceFactory.provisionGpio(gpio, mode, 0);
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice()");
		deviceFactory.deprovisionGpio(mock);
	}

	@Override
	public int getGpio() {
		return mock.getGpio();
	}

	@Override
	public void setMode(DeviceMode mode) {
		mock.setMode(mode);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return mock.getValue() == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		mock.setValue(value ? 1 : 0);
	}
}
