package com.diozero.internal;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;

public class PwmServoDevice extends AbstractDevice implements InternalServoDeviceInterface {
	private InternalPwmOutputDeviceInterface pwmOutputDevice;
	private int periodUs;

	public PwmServoDevice(String key, DeviceFactoryInterface deviceFactory,
			InternalPwmOutputDeviceInterface pwmOutputDevice, int minPulseWidthUs, int maxPulseWidthUs) {
		super(key, deviceFactory);

		this.pwmOutputDevice = pwmOutputDevice;
		pwmOutputDevice.setChild(true);
		periodUs = 1_000_000 / pwmOutputDevice.getPwmFrequency();
	}

	@Override
	public int getGpio() {
		return pwmOutputDevice.getGpio();
	}

	@Override
	public int getServoNum() {
		return pwmOutputDevice.getPwmNum();
	}

	@Override
	public int getPulseWidthUs() throws RuntimeIOException {
		return Math.round(pwmOutputDevice.getValue() * periodUs);
	}

	@Override
	public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
		pwmOutputDevice.setValue(pulseWidthUs / (float) periodUs);
	}

	@Override
	public int getServoFrequency() {
		return pwmOutputDevice.getPwmFrequency();
	}

	@Override
	public void setServoFrequency(int frequencyHz) throws RuntimeIOException {
		pwmOutputDevice.setPwmFrequency(frequencyHz);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		pwmOutputDevice.close();
	}
}
