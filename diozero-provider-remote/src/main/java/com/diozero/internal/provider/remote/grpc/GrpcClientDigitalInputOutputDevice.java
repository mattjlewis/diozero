package com.diozero.internal.provider.remote.grpc;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;

public class GrpcClientDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface {
	private GrpcClientDeviceFactory deviceFactory;
	private int gpio;
	private DeviceMode mode;

	public GrpcClientDigitalInputOutputDevice(GrpcClientDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			DeviceMode mode) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();

		deviceFactory.provisionDigitalInputOutputDevice(gpio, mode == DeviceMode.DIGITAL_OUTPUT);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return deviceFactory.digitalRead(gpio);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		deviceFactory.digitalWrite(gpio, value);
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
		deviceFactory.setOutput(gpio, mode == DeviceMode.DIGITAL_OUTPUT);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		deviceFactory.closeGpio(gpio);
	}

	@Override
	protected void enableListener() {
		deviceFactory.subscribe(gpio);
	}

	@Override
	protected void disableListener() {
		deviceFactory.unsubscribe(gpio);
	}
}
