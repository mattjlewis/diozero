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
import java.util.*;

import com.diozero.api.DeviceMode;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class OdroidBoardInfoProvider implements BoardInfoProvider {
	public static final OdroidC2BoardInfo ODROID_C2 = new OdroidC2BoardInfo(2048);
	
	public static final String MAKE = "Odroid";
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
		//BOARDS.put("000a", new OdroidBoardInfo(Model.C1, 1024));
		//BOARDS.put("0100", new OdroidBoardInfo(Model.XU_3_4, 2048));
		BOARD_REVISIONS.put("020b", ODROID_C2);
	}

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.equals(C2_HARDWARE_ID)) {
			return ODROID_C2;
		}
		return null;
	}

	public static class OdroidBoardInfo extends BoardInfo {
		public OdroidBoardInfo(Model model, int memory) {
			super(MAKE, model.toString(), memory, null, MAKE.toLowerCase() + "/" + model.toString().toLowerCase());
		}
		
		@Override
		public String getLibraryPath() {
			return MAKE.toLowerCase() + "/" + getModel().toLowerCase();
		}

		@Override
		public boolean isSupported(DeviceMode mode, int gpio) {
			return false;
		}
	}

	/**
	 * See <a href="http://www.hardkernel.com/main/products/prdt_info.php?g_code=G145457216438&tab_idx=2">Odroid C2 Hardware Technical details</a>.
	 */
	public static class OdroidC2BoardInfo extends BoardInfo {
		private static Map<Integer, List<DeviceMode>> C2_PINS;
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

			C2_PINS = new HashMap<>();
			C2_PINS.put(Integer.valueOf(214), digital_in_out);
			C2_PINS.put(Integer.valueOf(218), digital_in_out);
			C2_PINS.put(Integer.valueOf(219), digital_in_out);
			C2_PINS.put(Integer.valueOf(224), digital_in_out);
			C2_PINS.put(Integer.valueOf(225), digital_in_out);
			for (int i=228; i<=239; i++) {
				if (i == 234 || i == 235) {
					C2_PINS.put(Integer.valueOf(i), digital_in_out_pwm);
				} else {
					C2_PINS.put(Integer.valueOf(i), digital_in_out);
				}
			}
			C2_PINS.put(Integer.valueOf(247), digital_in_out);
			C2_PINS.put(Integer.valueOf(249), digital_in_out);
			// Note these are actual pin numbers, not logical GPIO numbers
			C2_PINS.put(Integer.valueOf(37), Arrays.asList(DeviceMode.ANALOG_INPUT));
			C2_PINS.put(Integer.valueOf(40), Arrays.asList(DeviceMode.ANALOG_INPUT));
		}

		private OdroidC2BoardInfo(int memory) {
			super(MAKE, Model.C2.toString(), memory, C2_PINS, MAKE.toLowerCase() + "/" + Model.C2.toString().toLowerCase());
		}
	}
}
