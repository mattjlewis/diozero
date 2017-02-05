package com.diozero.util;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.util.List;
import java.util.Map;

import com.diozero.api.DeviceMode;

public abstract class BoardPinInfo {
	private Map<Integer, List<DeviceMode>> pins;

	public BoardPinInfo(Map<Integer, List<DeviceMode>> pins) {
		this.pins = pins;
	}

	public boolean isSupported(DeviceMode mode, int pin) {
		// Default to true if the pins aren't defined
		if (pins == null) {
			return true;
		}
		
		List<DeviceMode> modes = pins.get(Integer.valueOf(pin));
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
