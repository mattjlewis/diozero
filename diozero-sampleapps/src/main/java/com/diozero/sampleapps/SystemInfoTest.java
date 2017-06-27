package com.diozero.sampleapps;

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


import java.util.Map;

import org.pmw.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.api.PwmPinInfo;
import com.diozero.util.BoardInfo;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.SystemInfo;

public class SystemInfoTest {
	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		Logger.info("Operating System id: {}", SystemInfo.getOperatingSystemId());
		Logger.info("Operating System version: {} {}", SystemInfo.getOperatingSystemVersion(),
				SystemInfo.getOperatingSystemVersionId());

		BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();

		Logger.info("Name: {}", board_info.getName());
		Logger.info("Make: {}", board_info.getMake());
		Logger.info("Model: {}", board_info.getModel());
		Logger.info("Memory: {}", Integer.valueOf(board_info.getMemory()));

		Logger.info("Pins:");
		for (Map<Integer, PinInfo> pins : board_info.getHeaders().values()) {
			for (PinInfo pin_info : pins.values()) {
				if (pin_info instanceof PwmPinInfo) {
					System.out.format("Pin [%s-%d]: %s %d (PWM%d) %s%n", pin_info.getHeader(), pin_info.getPinNumber(),
							pin_info.getName(), pin_info.getDeviceNumber(), ((PwmPinInfo) pin_info).getPwmNum(),
							pin_info.getModes().toString());
				} else {
					System.out.format("Pin [%s-%d]: %s %d %s%n", pin_info.getHeader(), pin_info.getPinNumber(),
							pin_info.getName(), pin_info.getDeviceNumber(), pin_info.getModes().toString());
				}
			}
		}

		Logger.info("I2C buses: {}", board_info.getI2CBuses());
		Logger.info("CPU Temperature: {0.##}", Float.valueOf(board_info.getCpuTemperature()));
	}
}
