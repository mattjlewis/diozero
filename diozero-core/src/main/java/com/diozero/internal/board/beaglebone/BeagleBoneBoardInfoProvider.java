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


import java.util.*;

import com.diozero.api.DeviceMode;
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
		private static final String BB_BLACK = "Black";
		private static final int MEMORY = 512;
		private static final String BBB_LIB_PATH = MAKE.toLowerCase() + "/" + BB_BLACK.toLowerCase();
		private static Map<Integer, List<DeviceMode>> BBB_PINS;
		static {
			List<DeviceMode> digital_in_out = Arrays.asList(
					DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT,
					DeviceMode.SOFTWARE_PWM_OUTPUT);

			BBB_PINS = new HashMap<>();
			BBB_PINS.put(Integer.valueOf(20), digital_in_out);
			BBB_PINS.put(Integer.valueOf(26), digital_in_out);
			BBB_PINS.put(Integer.valueOf(27), digital_in_out);
			BBB_PINS.put(Integer.valueOf(45), digital_in_out);
			BBB_PINS.put(Integer.valueOf(46), digital_in_out);
			BBB_PINS.put(Integer.valueOf(47), digital_in_out);
			BBB_PINS.put(Integer.valueOf(48), digital_in_out);
			BBB_PINS.put(Integer.valueOf(49), digital_in_out);
			BBB_PINS.put(Integer.valueOf(60), digital_in_out);
			BBB_PINS.put(Integer.valueOf(61), digital_in_out);
			BBB_PINS.put(Integer.valueOf(66), digital_in_out);
			BBB_PINS.put(Integer.valueOf(67), digital_in_out);
			BBB_PINS.put(Integer.valueOf(68), digital_in_out);
			BBB_PINS.put(Integer.valueOf(69), digital_in_out);
			BBB_PINS.put(Integer.valueOf(112), digital_in_out);
			BBB_PINS.put(Integer.valueOf(115), digital_in_out);
			BBB_PINS.put(Integer.valueOf(117), digital_in_out);
		}
		
		public BeagleBoneBlackBoardInfo() {
			super(MAKE, BB_BLACK, MEMORY, BBB_PINS, BBB_LIB_PATH);
		}
	}
}
