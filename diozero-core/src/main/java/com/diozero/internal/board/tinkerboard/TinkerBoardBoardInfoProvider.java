package com.diozero.internal.board.tinkerboard;

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

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.mmap.MmapGpioInterface;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class TinkerBoardBoardInfoProvider implements BoardInfoProvider {
	public static final TinkerBoardBoardInfo TINKER_BOARD = new TinkerBoardBoardInfo();
	public static final String MAKE = "Asus";
	private static final String TINKER_BOARD_HARDWARE_ID = "Rockchip (Device Tree)";

	@Override
	public BoardInfo lookup(String hardware, String revision, Integer memoryKb) {
		if (hardware != null && hardware.equals(TINKER_BOARD_HARDWARE_ID)) {
			return TINKER_BOARD;
		}
		return null;
	}
	
	public static class TinkerBoardBoardInfo extends BoardInfo {
		public static final String MODEL = "Tinker Board";
		private static final int MEMORY = 2048;
		private static final String LIBRARY_PATH = "tinkerboard";
		
		private TinkerBoardBoardInfo() {
			super(MAKE, MODEL, MEMORY, LIBRARY_PATH);

			// GPIO0_C1
			addGpioPinInfo(16+1, "GPIO0_C1", 7, PinInfo.DIGITAL_IN_OUT);
			
			// GPIO5B (GP5B0-GP5B7)
			addGpioPinInfo(160, "GPIO5_B0", 10, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+1, "GPIO5_B1", 8, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+2, "GPIO5_B2", 16, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+3, "GPIO5_B3", 18, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+4, "GPIO5_B4", 11, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+5, "GPIO5_B5", 29, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+6, "GPIO5_B6", 13, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(160+7, "GPIO5_B7", 15, PinInfo.DIGITAL_IN_OUT);
			
			// GPIO5C (GP5C0-GP5C3)
			addGpioPinInfo(168, "GPIO5_C0", 31, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(168+1, "GPIO5_C1", -1, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(168+2, "GPIO5_C2", -1, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(168+3, "GPIO5_C3", 22, PinInfo.DIGITAL_IN_OUT);

			// GPIO6A (GP6A0-GP6A1)
			addGpioPinInfo(184, "GPIO6_A0", 12, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(184+1, "GPIO6_A1", 35, PinInfo.DIGITAL_IN_OUT);
			// GPIO6A (GP6A3-GP6A4)
			addGpioPinInfo(184+3, "GPIO6_A3", 38, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(184+4, "GPIO6_A4", 40, PinInfo.DIGITAL_IN_OUT);
			
			// GPIO7A (GP7A7)
			addGpioPinInfo(216+7, "GPIO7_A7", 36, PinInfo.DIGITAL_IN_OUT);
			
			// GPIO7B (GP7B0-GP7B2)
			addGpioPinInfo(224, "GPIO7_B0", 37, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(224+1, "GPIO7_B1", -1, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(224+2, "GPIO7_B2", -1, PinInfo.DIGITAL_IN_OUT);
			
			// GPIO7CL (GP7C1-GP7C2)
			addGpioPinInfo(232+1, "GPIO7_C1", 27, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(232+2, "GPIO7_C2", 28, PinInfo.DIGITAL_IN_OUT);
			// GPIO7CH (GP7C6-GP7C7)
			addPwmPinInfo(232+6, "GPIO7_C6", 33, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(232+7, "GPIO7_C7", 32, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			
			// GPIO8A (GP8A3-GP8A7)
			addGpioPinInfo(248+3, "GPIO8_A3", 26, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(248+4, "GPIO8_A4", 3, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(248+5, "GPIO8_A5", 5, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(248+6, "GPIO8_A6", 23, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(248+7, "GPIO8_A7", 24, PinInfo.DIGITAL_IN_OUT);

			// GPIO8B (GP8B0-GP8B1)
			addGpioPinInfo(256, "GPIO8_B0", 21, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(256+1, "GPIO8_B1", 19, PinInfo.DIGITAL_IN_OUT);
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
		
		@Override
		public MmapGpioInterface createMmapGpio() {
			return new TinkerBoardMmapGpio();
		}
	}
}
