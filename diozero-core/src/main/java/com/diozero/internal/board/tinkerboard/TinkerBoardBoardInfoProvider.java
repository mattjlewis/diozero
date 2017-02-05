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


import java.util.*;

import com.diozero.api.DeviceMode;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class TinkerBoardBoardInfoProvider implements BoardInfoProvider {
	public static final TinkerBoardBoardInfo TINKER_BOARD = new TinkerBoardBoardInfo();
	private static final String TINKER_BOARD_HARDWARE_ID = "Rockchip (Device Tree)";

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.equals(TINKER_BOARD_HARDWARE_ID)) {
			return TINKER_BOARD;
		}
		return null;
	}
	
	public static class TinkerBoardBoardInfo extends BoardInfo {
		private static final String MAKE = "Asus";
		private static final String MODEL = "Tinker Board";
		private static final int MEMORY = 2048;
		private static final String LIBRARY_PATH = "tinkerboard";
		
		private static Map<Integer, List<DeviceMode>> PINS;
		static {
			List<DeviceMode> digital_in_out = Arrays.asList(
					DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT,
					DeviceMode.SOFTWARE_PWM_OUTPUT);
			// See http://odroid.com/dokuwiki/doku.php?id=en:c2_hardware_pwm
			List<DeviceMode> digital_in_out_pwm = Arrays.asList(
					DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT,
					DeviceMode.SOFTWARE_PWM_OUTPUT,
					DeviceMode.PWM_OUTPUT);

			PINS = new HashMap<>();
			// GPIO5B (GP5B0-GP5B7)
			for (int i=160; i<168; i++) {
				PINS.put(Integer.valueOf(i), digital_in_out);
			}
			// GPIO5C (GP5C0-GP5C3)
			for (int i=168; i<172; i++) {
				PINS.put(Integer.valueOf(i), digital_in_out);
			}
			// GPIO6A (GP6A0-GP6A4)
			PINS.put(Integer.valueOf(184), digital_in_out);
			PINS.put(Integer.valueOf(185), digital_in_out);
			PINS.put(Integer.valueOf(187), digital_in_out);
			PINS.put(Integer.valueOf(188), digital_in_out);
			// GPIO7A
			PINS.put(Integer.valueOf(223), digital_in_out);
			// GPIO7B
			PINS.put(Integer.valueOf(224), digital_in_out);
			PINS.put(Integer.valueOf(225), digital_in_out);
			PINS.put(Integer.valueOf(226), digital_in_out);
			// GPIO7CL
			PINS.put(Integer.valueOf(233), digital_in_out);
			PINS.put(Integer.valueOf(234), digital_in_out);
			// GPIO7CH
			PINS.put(Integer.valueOf(238), digital_in_out_pwm);
			PINS.put(Integer.valueOf(239), digital_in_out_pwm);
			// GPIO8A
			for (int i=251; i<258; i++) {
				PINS.put(Integer.valueOf(i), digital_in_out);
			}
		}
		
		private TinkerBoardBoardInfo() {
			super(MAKE, MODEL, MEMORY, PINS, LIBRARY_PATH);
		}
	}
}
