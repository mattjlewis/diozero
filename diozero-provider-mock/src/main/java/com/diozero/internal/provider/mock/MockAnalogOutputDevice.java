package com.diozero.internal.provider.mock;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;

public class MockAnalogOutputDevice extends AbstractDevice implements AnalogOutputDeviceInterface {
	private static final int INT_RANGE = 256;

	private final MockDeviceFactory deviceFactory;
	private final MockGpio mock;

	public MockAnalogOutputDevice(String key, MockDeviceFactory deviceFactory, int gpio, float initialValue) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		mock = deviceFactory.provisionGpio(gpio, DeviceMode.ANALOG_OUTPUT, (int) initialValue * INT_RANGE);
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice()");
		deviceFactory.deprovisionGpio(mock);
	}

	@Override
	public int getAdcNumber() {
		return mock.getGpio();
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return mock.getValue() / (float) INT_RANGE;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		mock.setValue((int) value * INT_RANGE);
	}
}
