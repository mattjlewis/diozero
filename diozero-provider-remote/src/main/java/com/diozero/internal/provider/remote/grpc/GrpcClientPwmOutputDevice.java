package com.diozero.internal.provider.remote.grpc;

import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.PwmOutputDeviceInterface;

public class GrpcClientPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private GrpcClientDeviceFactory deviceFactory;
	private int gpio;

	public GrpcClientPwmOutputDevice(GrpcClientDeviceFactory deviceFactory, String key, PinInfo pinInfo, int frequency,
			float initialValue) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();

		deviceFactory.provisionPwmOutputDevice(gpio, frequency, initialValue);
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
		return deviceFactory.pwmRead(gpio);
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		deviceFactory.pwmWrite(gpio, value);
	}

	@Override
	public int getPwmFrequency() {
		return deviceFactory.getPwmFrequency(gpio);
	}

	@Override
	public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
		deviceFactory.setPwmFrequency(gpio, frequencyHz);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		deviceFactory.closeGpio(gpio);
	}
}
