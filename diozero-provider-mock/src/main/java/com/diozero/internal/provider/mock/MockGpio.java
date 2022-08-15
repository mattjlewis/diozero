package com.diozero.internal.provider.mock;

import com.diozero.api.DeviceMode;

public class MockGpio {
	private int gpio;
	private DeviceMode mode;
	private int value;

	public MockGpio(int gpio, DeviceMode mode, int value) {
		this.gpio = gpio;
		this.mode = mode;
		this.value = value;
	}

	public int getGpio() {
		return gpio;
	}

	public DeviceMode getMode() {
		return mode;
	}

	public void setMode(DeviceMode mode) {
		this.mode = mode;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}
