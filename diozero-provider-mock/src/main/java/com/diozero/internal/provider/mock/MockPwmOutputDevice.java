package com.diozero.internal.provider.mock;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;

public class MockPwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface {
	private final MockDeviceFactory deviceFactory;
	private final MockGpio mock;
	private int pwmNum;
	private int frequency;

	public MockPwmOutputDevice(String key, MockDeviceFactory deviceFactory, int gpio, int pwmNum, int frequency,
			float initialValue) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		mock = deviceFactory.provisionGpio(gpio, DeviceMode.PWM_OUTPUT, (int) initialValue * frequency);
		this.pwmNum = pwmNum;
		this.frequency = frequency;
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
	public int getPwmNum() {
		return pwmNum;
	}

	@Override
	public int getPwmFrequency() throws RuntimeIOException {
		return frequency;
	}

	@Override
	public void setPwmFrequency(int frequency) throws RuntimeIOException {
		this.frequency = frequency;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return mock.getValue() / (float) frequency;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		Logger.debug("setValue({})", Float.valueOf(value));
		mock.setValue((int) value * frequency);
	}
}
