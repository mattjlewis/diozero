package com.diozero.sbc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     UnknownBoardInfo.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import java.util.Optional;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.board.GenericLinuxArmBoardInfo;

/**
 * Attempt to handle generic boards that don't have explicit support within diozero
 */
public class UnknownBoardInfo extends BoardInfo {
	public static final float DEFAULT_ADC_VREF = 1.8f;

	public static BoardInfo get(LocalSystemInfo localSysInfo) {
		if (localSysInfo.isLinux() && localSysInfo.isArm()) {
			Logger.debug("Using generic Linux board info, make '{}', model '{}', SoC '{}'", localSysInfo.getMake(),
					localSysInfo.getModel(), localSysInfo.getSoc());
			return new GenericLinuxArmBoardInfo(localSysInfo);
		}

		Logger.warn(
				"Unable to resolve board provider for make '{}', model '{}', SoC '{}', on non-Linux local O/S: {}-{}",
				localSysInfo.getMake(), localSysInfo.getModel(), localSysInfo.getSoc(), localSysInfo.getOsName(),
				localSysInfo.getOsArch());

		return new UnknownBoardInfo(localSysInfo);
	}

	private UnknownBoardInfo(LocalSystemInfo localSysInfo) {
		super(localSysInfo.getMake(), localSysInfo.getModel(),
				localSysInfo.getMemoryKb() == null ? -1 : localSysInfo.getMemoryKb().intValue(),
				localSysInfo.getOperatingSystemId(), localSysInfo.getOperatingSystemVersion());
	}

	@Override
	public boolean isRecognised() {
		return false;
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
	public Optional<PinInfo> getByGpioNumber(int gpio) {
		return Optional
				.of(super.getByGpioNumber(gpio).orElseGet(() -> addGpioPinInfo(gpio, gpio, PinInfo.DIGITAL_IN_OUT)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<PinInfo> getByAdcNumber(int adcNumber) {
		return Optional.of(
				super.getByAdcNumber(adcNumber).orElseGet(() -> addAdcPinInfo(adcNumber, adcNumber, DEFAULT_ADC_VREF)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<PinInfo> getByDacNumber(int dacNumber) {
		return Optional.of(super.getByDacNumber(dacNumber).orElseGet(() -> addDacPinInfo(dacNumber, dacNumber)));
	}
}
