package com.diozero.internal.board.beaglebone;

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

import java.io.IOException;
import java.nio.file.*;

import org.pmw.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class BeagleBoneBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "BeagleBone";
	private static final String BBB_HARDWARE_ID = "Generic AM33XX";
	public static final BoardInfo BBB_BOARD_INFO = new BeagleBoneBlackBoardInfo();

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.startsWith(BBB_HARDWARE_ID)) {
			return BBB_BOARD_INFO;
		}
		return null;
	}
	
	public static class BeagleBoneBlackBoardInfo extends BoardInfo {
		public static final String P9_HEADER = "P9";
		public static final String P8_HEADER = "P8";
		
		private static final String BB_BLACK = "Black";
		private static final int MEMORY = 512;
		private static final String BBB_LIB_PATH = MAKE.toLowerCase() + "/" + BB_BLACK.toLowerCase();
		
		public BeagleBoneBlackBoardInfo() {
			super(MAKE, BB_BLACK, MEMORY, BBB_LIB_PATH);

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
		}

		@Override
		public int getPwmChip(int pwmNum) {
			// FIXME How to work this out? Temporarily hardcode to GPIO 50 (EHRPWM1A, P9_14)
			String chip = "48302000";
			String address = "48302200";
			
			Path chip_path = FileSystems.getDefault().getPath("/sys/devices/platform/ocp/" + chip + ".epwmss/" + address + ".pwm/pwm");
			int pwm_chip = -1;
			try (DirectoryStream<Path> dirs = Files.newDirectoryStream(chip_path, "pwm*")) {
				for (Path p : dirs) {
					String dir = p.getFileName().toString();
					Logger.info("Got {}" + dir);
					pwm_chip = Integer.parseInt(dir.substring(dir.length()-1));
					Logger.info("Found pwmChip {}", Integer.valueOf(pwm_chip));
				}
			} catch (IOException e) {
				Logger.error(e, "Error: " + e);
			}
			
			return pwm_chip;
		}
	}
}
