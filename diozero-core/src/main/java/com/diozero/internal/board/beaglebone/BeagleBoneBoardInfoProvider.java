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

import com.diozero.api.GpioInfo;
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
		}

		@Override
		protected void init() {
			addGpioInfo(new GpioInfo(P9_HEADER, 60, 12, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P9_HEADER, 48, 15, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P9_HEADER, 49, 23, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P9_HEADER, 117, 25, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P9_HEADER, 115, 27, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P9_HEADER, 112, 30, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P9_HEADER, 20, 41, GpioInfo.DIGITAL_IN_OUT));

			addGpioInfo(new GpioInfo(P8_HEADER, 66, 7, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 67, 8, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 69, 9, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 68, 10, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 45, 11, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 44, 12, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 26, 14, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 47, 15, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 46, 16, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 27, 17, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 65, 18, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(P8_HEADER, 61, 26, GpioInfo.DIGITAL_IN_OUT));
		}
	}
}
