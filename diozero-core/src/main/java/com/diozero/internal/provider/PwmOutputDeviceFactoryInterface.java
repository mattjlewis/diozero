package com.diozero.internal.provider;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     PwmOutputDeviceFactoryInterface.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.DeviceMode;
import com.diozero.api.InvalidModeException;
import com.diozero.api.PinInfo;
import com.diozero.util.RuntimeIOException;

public interface PwmOutputDeviceFactoryInterface extends DeviceFactoryInterface {
	default PwmOutputDeviceInterface provisionPwmOutputDevice(int pwmOrGpioNum, int pwmFrequency, float initialValue)
			throws RuntimeIOException {
		// Lookup by PWM number first, if not found or doesn't support
		// PWM_OUTPUT, lookup by GPIO number
		PinInfo pin_info = getBoardPinInfo().getByPwmNumber(pwmOrGpioNum);
		if (pin_info == null || !pin_info.isSupported(DeviceMode.PWM_OUTPUT)) {
			pin_info = getBoardPinInfo().getByGpioNumber(pwmOrGpioNum);
		}
		if (pin_info != null && pin_info.isSupported(DeviceMode.PWM_OUTPUT)) {
			// Ok
		} else if (pin_info != null && pin_info.isSupported(DeviceMode.DIGITAL_OUTPUT)) {
			Logger.warn("Hardware PWM not available on pin {}, reverting to software", Integer.valueOf(pwmOrGpioNum));
		} else {
			throw new InvalidModeException("Invalid mode (PWM output) for GPIO " + pwmOrGpioNum);
		}

		String key = createPinKey(pin_info);

		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}

		PwmOutputDeviceInterface device = createPwmOutputDevice(key, pin_info, pwmFrequency, initialValue);
		deviceOpened(device);

		return device;
	}

	int getBoardPwmFrequency();
	void setBoardPwmFrequency(int pwmFrequency);
	PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency, float initialValue);
}
