package com.diozero.internal.provider.mock;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

public class MockDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private final MockDeviceFactory deviceFactory;
	private final MockGpio mock;

	public MockDigitalOutputDevice(String key, MockDeviceFactory deviceFactory, int gpio, boolean initialValue) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		mock = deviceFactory.provisionGpio(gpio, DeviceMode.DIGITAL_OUTPUT, initialValue ? 1 : 0);
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
	public boolean getValue() throws RuntimeIOException {
		return mock.getValue() == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		Logger.debug("setValue({})", Boolean.valueOf(value));
		mock.setValue(value ? 1 : 0);
	}
}
