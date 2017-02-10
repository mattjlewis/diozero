package com.diozero.internal.board.odroid;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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
import java.util.HashMap;
import java.util.Map;

import com.diozero.api.GpioInfo;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class OdroidBoardInfoProvider implements BoardInfoProvider {
	public static final OdroidC1BoardInfo ODROID_C1 = new OdroidC1BoardInfo();
	public static final OdroidC2BoardInfo ODROID_C2 = new OdroidC2BoardInfo();
	
	public static final String MAKE = "Odroid";
	private static final String C1_HARDWARE_ID = "ODROID-C1";
	private static final String C2_HARDWARE_ID = "ODROID-C2";
	
	public static enum Model {
		C0, U2_U3, C1, XU_3_4, C2;
	}
	
	private static Map<String, BoardInfo> BOARD_REVISIONS;
	static {
		BOARD_REVISIONS = new HashMap<>();
		
		// TODO Verify C0
		//BOARDS.put("????", new OdroidBoardInfo(Model.C0, 1024));
		//BOARDS.put("0000", new OdroidBoardInfo(Model.U2_U3, 2048));
		BOARD_REVISIONS.put("000a", ODROID_C1);
		//BOARDS.put("0100", new OdroidBoardInfo(Model.XU_3_4, 2048));
		BOARD_REVISIONS.put("020b", ODROID_C2);
	}

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null) {
			if (hardware.equals(C2_HARDWARE_ID)) {
				return ODROID_C2;
			} else if (hardware.equals(C1_HARDWARE_ID)) {
				return ODROID_C1;
			}
		}
		return null;
	}

	public static class OdroidC1BoardInfo extends BoardInfo {
		private static final int MEMORY = 1024;
		
		public OdroidC1BoardInfo() {
			super(MAKE, Model.C1.toString(), MEMORY, MAKE.toLowerCase() + "/" + Model.C1.toString().toLowerCase());
		}
		
		@Override
		protected void init() {
			// TODO Add pins when support for this board is added
		}
	}

	/**
	 * See <a href="http://www.hardkernel.com/main/products/prdt_info.php?g_code=G145457216438&tab_idx=2">Odroid C2 Hardware Technical details</a>.
	 */
	public static class OdroidC2BoardInfo extends BoardInfo {
		private static final int MEMORY = 2048;
		
		private OdroidC2BoardInfo() {
			super(MAKE, Model.C2.toString(), MEMORY, MAKE.toLowerCase() + "/" + Model.C2.toString().toLowerCase());
		}
		
		@Override
		protected void init() {
			addGpioInfo(new GpioInfo(214,35, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(218, 36, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(219, 31, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(224, 32, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(225, 26, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(228, 29, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(229, 24, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(230, 23, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(231, 22, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(232, 21, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(233, 18, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(234, 33, GpioInfo.DIGITAL_IN_OUT_PWM));
			addGpioInfo(new GpioInfo(235, 19, GpioInfo.DIGITAL_IN_OUT_PWM));
			addGpioInfo(new GpioInfo(247, 11, GpioInfo.DIGITAL_IN_OUT));
			addGpioInfo(new GpioInfo(249, 7, GpioInfo.DIGITAL_IN_OUT));
			// Note these are actual pin numbers, not logical GPIO numbers
			addGpioInfo(new GpioInfo(37, 37, GpioInfo.ANALOG_INPUT));
			addGpioInfo(new GpioInfo(40, 40, GpioInfo.ANALOG_INPUT));
		}
	}
}
