package com.diozero.internal.board.beaglebone;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     BeagleBoneBoardInfoProvider.java  
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.spi.BoardInfoProvider;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;

public class BeagleBoneBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "BeagleBone";
	// private static final String BBB_HARDWARE_ID = "Generic AM33XX";

	@Override
	public BoardInfo lookup(LocalSystemInfo localSysInfo) {
		String model = localSysInfo.getModel();
		if (model != null && model.contains(MAKE)) {
			model = model.substring(model.lastIndexOf(' ') + 1);
			return new BeagleBoneBlackBoardInfo(localSysInfo, model);
		}
		return null;
	}

	/**
	 * Also works on the BeagleBone Green
	 */
	public static class BeagleBoneBlackBoardInfo extends GenericLinuxArmBoardInfo {
		public static final String P9_HEADER = "P9";
		public static final String P8_HEADER = "P8";

		private static final int MEMORY = 512_000;
		private static final float ADC_VREF = 1.8f;

		public BeagleBoneBlackBoardInfo(LocalSystemInfo localSysInfo, String model) {
			super(MAKE, model, MEMORY, ADC_VREF);
		}

		@Override
		public void populateBoardPinInfo() {
			// FIXME Externalise this to a file
			addGpioPinInfo(P9_HEADER, 60, 12, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 48, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 49, 23, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 117, 25, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 115, 27, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 112, 30, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 20, 41, PinInfo.DIGITAL_IN_OUT);

			addGpioPinInfo(P8_HEADER, 66, 7, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 67, 8, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 69, 9, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 68, 10, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 45, 11, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 44, 12, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 26, 14, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 47, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 46, 16, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 27, 17, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 65, 18, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 61, 26, PinInfo.DIGITAL_IN_OUT);

			/*- To enable: sudo sh -c "echo 'BB-ADC' > /sys/devices/platform/bone_capemgr/slots" */
			addAdcPinInfo(P9_HEADER, 0, "AIN0", 39);
			addAdcPinInfo(P9_HEADER, 1, "AIN1", 40);
			addAdcPinInfo(P9_HEADER, 2, "AIN2", 37);
			addAdcPinInfo(P9_HEADER, 3, "AIN3", 38);
			addAdcPinInfo(P9_HEADER, 4, "AIN4", 33);
			addAdcPinInfo(P9_HEADER, 5, "AIN5", 36);
			addAdcPinInfo(P9_HEADER, 6, "AIN6", 35);

			// BB-PWM0,BB-PWM1,BB-PWM2
			addPwmPinInfo(P9_HEADER, PinInfo.NOT_DEFINED, "EHRPWM1A", 14, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(P9_HEADER, PinInfo.NOT_DEFINED, "EHRPWM1B", 16, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(P8_HEADER, PinInfo.NOT_DEFINED, "EHRPWM2A", 13, 3, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(P8_HEADER, PinInfo.NOT_DEFINED, "EHRPWM2B", 19, 4, PinInfo.DIGITAL_IN_OUT_PWM);
		}

		@Override
		public int getPwmChip(int pwmNum) {
			// FIXME How to work this out? Temporarily hardcode to GPIO 50 (EHRPWM1A, P9_14)
			String chip = "48302000";
			String address = "48302200";

			Path chip_path = Paths.get("/sys/devices/platform/ocp/" + chip + ".epwmss/" + address + ".pwm/pwm");
			int pwm_chip = -1;
			// FIXME Treat as a stream
			try (DirectoryStream<Path> dirs = Files.newDirectoryStream(chip_path, "pwm*")) {
				for (Path p : dirs) {
					String dir = p.getFileName().toString();
					Logger.info("Got {}" + dir);
					pwm_chip = Integer.parseInt(dir.substring(dir.length() - 1));
					Logger.info("Found pwmChip {}", Integer.valueOf(pwm_chip));
				}
			} catch (IOException e) {
				Logger.error(e, "Error: " + e);
			}

			return pwm_chip;
		}

		/*-
		 * @Override public MmapGpioInterface createMmapGpio() {
		 * 	return new BeagleBoneBlackMmapGpio();
		 * }
		 */
	}
}
