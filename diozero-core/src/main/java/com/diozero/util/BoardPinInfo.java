package com.diozero.util;

import java.util.List;
import java.util.Map;

import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;

public abstract class BoardPinInfo {
	private Map<Integer, List<GpioDeviceInterface.Mode>> pins;

	public BoardPinInfo(Map<Integer, List<GpioDeviceInterface.Mode>> pins) {
		this.pins = pins;
	}

	public boolean isSupported(Mode mode, int pin) {
		// Default to true if the pins aren't defined
		if (pins == null) {
			return true;
		}
		
		List<Mode> modes = pins.get(Integer.valueOf(pin));
		// Default to true if the modes for the requested pin isn't set
		if (modes == null) {
			return true;
		}
		
		return modes.contains(mode);
	}
	
	@SuppressWarnings("static-method")
	public int mapGpio(int gpio) {
		return gpio;
	}
}
