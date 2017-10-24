package com.diozero.remote.message;

import java.io.Serializable;
import java.util.Collection;

import com.diozero.api.DeviceMode;

public class GpioInfo implements Serializable{
	private static final long serialVersionUID = -2314562135065372805L;

	private int gpio;
	private Collection<DeviceMode> modes;
	
	public GpioInfo(int gpio, Collection<DeviceMode> modes) {
		this.gpio = gpio;
		this.modes = modes;
	}

	public int getGpio() {
		return gpio;
	}

	public Collection<DeviceMode> getModes() {
		return modes;
	}
}
