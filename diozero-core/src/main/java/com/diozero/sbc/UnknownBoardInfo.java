package com.diozero.sbc;

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
import com.diozero.internal.board.GenericLinuxArmBoardInfo;

/**
 * Attempt to handle generic boards that don't have explicit support within
 * diozero
 */
public class UnknownBoardInfo extends BoardInfo {
	public static BoardInfo get(LocalSystemInfo localSysInfo) {
		String os_name = localSysInfo.getOsName();
		String os_arch = localSysInfo.getOsArch();
		Logger.warn("Failed to resolve board info for hardware '{}' and revision '{}'. Local O/S: {}-{}",
				localSysInfo.getHardware(), localSysInfo.getRevision(), os_name, os_arch);

		if (localSysInfo.isLinux() && localSysInfo.isArm()) {
			return new GenericLinuxArmBoardInfo(localSysInfo);
		}

		return new UnknownBoardInfo(localSysInfo);
	}

	public UnknownBoardInfo(LocalSystemInfo localSysInfo) {
		super(UNKNOWN, localSysInfo.getModel(),
				localSysInfo.getMemoryKb() == null ? -1 : localSysInfo.getMemoryKb().intValue(),
				localSysInfo.getDefaultLibraryPath());
	}
	
	public UnknownBoardInfo(String make, String model, int memoryKb, String libraryPath) {
		super(make, model, memoryKb, libraryPath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void populateBoardPinInfo() {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PinInfo getByGpioNumber(int gpio) {
		PinInfo pin_info = super.getByGpioNumber(gpio);
		if (pin_info == null) {
			pin_info = addGpioPinInfo(gpio, gpio, PinInfo.DIGITAL_IN_OUT);
		}
		return pin_info;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PinInfo getByAdcNumber(int adcNumber) {
		PinInfo pin_info = super.getByAdcNumber(adcNumber);
		if (pin_info == null) {
			pin_info = addAdcPinInfo(adcNumber, adcNumber);
		}
		return pin_info;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PinInfo getByDacNumber(int dacNumber) {
		PinInfo pin_info = super.getByDacNumber(dacNumber);
		if (pin_info == null) {
			pin_info = addDacPinInfo(dacNumber, dacNumber);
		}
		return pin_info;
	}
}
