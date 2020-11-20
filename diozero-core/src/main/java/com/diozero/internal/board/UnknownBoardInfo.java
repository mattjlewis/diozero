package com.diozero.internal.board;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     UnknownBoardInfo.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.util.BoardInfo;
import com.diozero.util.SystemInfoConstants;

public class UnknownBoardInfo extends BoardInfo {
	public static BoardInfo get(String model, String hardware, String revision, Integer memoryKb) {
		String os_name = System.getProperty(SystemInfoConstants.OS_NAME_SYSTEM_PROPERTY);
		String os_arch = System.getProperty(SystemInfoConstants.OS_ARCH_SYSTEM_PROPERTY);
		Logger.warn("Failed to resolve board info for hardware '{}' and revision '{}'. Local O/S: {}-{}", hardware,
				revision, os_name, os_arch);

		if (os_name.equals(SystemInfoConstants.LINUX_OS_NAME) && (os_arch.equals(SystemInfoConstants.ARM_32_OS_ARCH)
				|| os_arch.equals(SystemInfoConstants.ARM_64_OS_ARCH))) {
			return new GenericLinuxArmBoardInfo(UNKNOWN, model, memoryKb, os_name, os_arch);
		}

		return new UnknownBoardInfo(model, memoryKb, os_name, os_arch);
	}

	public UnknownBoardInfo(String model, Integer memoryKb, String osName, String osArch) {
		this(UNKNOWN, model, memoryKb, osName, osArch);
	}

	public UnknownBoardInfo(String make, String model, Integer memoryKb, String osName, String osArch) {
		this(make, model, memoryKb, osName.replace(" ", "").toLowerCase() + "-" + osArch.toLowerCase());
	}

	public UnknownBoardInfo(String make, String model, Integer memoryKb, String libraryPath) {
		super(make, model, memoryKb == null ? -1 : memoryKb.intValue(), libraryPath);
	}

	@Override
	public void initialisePins() {
	}

	@Override
	public PinInfo getByGpioNumber(int gpio) {
		PinInfo pin_info = super.getByGpioNumber(gpio);
		if (pin_info == null) {
			pin_info = addGpioPinInfo(gpio, gpio, PinInfo.DIGITAL_IN_OUT);
		}
		return pin_info;
	}

	@Override
	public PinInfo getByAdcNumber(int adcNumber) {
		PinInfo pin_info = super.getByAdcNumber(adcNumber);
		if (pin_info == null) {
			pin_info = addAdcPinInfo(adcNumber, adcNumber);
		}
		return pin_info;
	}

	@Override
	public PinInfo getByDacNumber(int dacNumber) {
		PinInfo pin_info = super.getByDacNumber(dacNumber);
		if (pin_info == null) {
			pin_info = addDacPinInfo(dacNumber, dacNumber);
		}
		return pin_info;
	}
}