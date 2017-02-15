package com.diozero.api;

import java.util.EnumSet;

public class PwmPinInfo extends PinInfo {
	private int pwmNum;

	public PwmPinInfo(String keyPrefix, String header, int gpioNumber, int pinNumber, int pwmNum, String name,
			EnumSet<DeviceMode> modes) {
		super(keyPrefix, header, gpioNumber, pinNumber, name, modes);

		this.pwmNum = pwmNum;
	}

	public int getPwmNum() {
		return pwmNum;
	}
}
