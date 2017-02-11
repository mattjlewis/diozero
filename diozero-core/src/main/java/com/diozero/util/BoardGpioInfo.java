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
import java.util.*;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioInfo;

public abstract class BoardGpioInfo {
	private Map<Integer, List<DeviceMode>> gpios;
	private Map<Integer, GpioInfo> gpiosByGpioNumber;
	private Map<String, GpioInfo> gpiosByName;
	private Map<Integer, GpioInfo> gpiosByPin;
	
	public BoardGpioInfo() {
		gpios = new HashMap<>();
		gpiosByGpioNumber = new HashMap<>();
		gpiosByName = new HashMap<>();
		gpiosByPin = new HashMap<>();
		
		init();
	}
	
	protected abstract void init();
	
	protected void addGpioInfo(GpioInfo gpioInfo) {
		gpios.put(Integer.valueOf(gpioInfo.getGpioNum()), gpioInfo.getModes());
		gpiosByGpioNumber.put(Integer.valueOf(gpioInfo.getGpioNum()), gpioInfo);
		gpiosByName.put(gpioInfo.getName(), gpioInfo);
		gpiosByPin.put(Integer.valueOf(gpioInfo.getPin()), gpioInfo);
	}
	
	public Set<Integer> getGioNumbers() {
		return gpiosByGpioNumber.keySet();
	}
	
	public Collection<GpioInfo> getGpios() {
		return gpiosByGpioNumber.values();
	}
	
	public GpioInfo getByGpioNumber(int gpio) {
		return gpiosByGpioNumber.get(Integer.valueOf(gpio));
	}
	
	public GpioInfo getByName(String name) {
		return gpiosByGpioNumber.get(name);
	}
	
	public GpioInfo getByPin(int pin) {
		return gpiosByGpioNumber.get(Integer.valueOf(pin));
	}

	public boolean isSupported(DeviceMode mode, int gpio) {
		// Default to true if the gpios aren't defined
		if (gpios == null) {
			return true;
		}
		
		List<DeviceMode> modes = gpios.get(Integer.valueOf(gpio));
		// Default to true if the modes for the requested gpios isn't found
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
