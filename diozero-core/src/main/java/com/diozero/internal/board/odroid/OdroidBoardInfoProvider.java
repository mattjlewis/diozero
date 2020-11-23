package com.diozero.internal.board.odroid;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     OdroidBoardInfoProvider.java  
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

import java.util.HashMap;
import java.util.Map;

import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.spi.BoardInfoProvider;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;

public class OdroidBoardInfoProvider implements BoardInfoProvider {
	public static final OdroidC1BoardInfo ODROID_C1 = new OdroidC1BoardInfo();
	public static final OdroidC2BoardInfo ODROID_C2 = new OdroidC2BoardInfo();

	public static final String MAKE = "Odroid";
	public static final String C1_HARDWARE_ID = "ODROID-C1";
	public static final String C2_HARDWARE_ID = "ODROID-C2";

	public static enum Model {
		C0, U2_U3, C1, XU_3_4, C2;
	}

	private static Map<String, BoardInfo> BOARD_REVISIONS;
	static {
		BOARD_REVISIONS = new HashMap<>();

		// TODO Verify C0
		// BOARDS.put("????", new OdroidBoardInfo(Model.C0, 1024));
		// BOARDS.put("0000", new OdroidBoardInfo(Model.U2_U3, 2048));
		BOARD_REVISIONS.put("000a", ODROID_C1);
		// BOARDS.put("0100", new OdroidBoardInfo(Model.XU_3_4, 2048));
		BOARD_REVISIONS.put("020b", ODROID_C2);
	}

	@Override
	public BoardInfo lookup(String hardware, String revision, Integer memoryKb) {
		if (hardware != null) {
			if (hardware.equals(C2_HARDWARE_ID) || hardware.endsWith(C2_HARDWARE_ID)) {
				return ODROID_C2;
			} else if (hardware.equals(C1_HARDWARE_ID)) {
				return ODROID_C1;
			}
		}
		return null;
	}

	public static class OdroidC1BoardInfo extends GenericLinuxArmBoardInfo {
		private static final int MEMORY = 1_024_000;

		public OdroidC1BoardInfo() {
			super(MAKE, Model.C1.toString(), Integer.valueOf(MEMORY),
					MAKE.toLowerCase() + "-" + Model.C1.toString().toLowerCase());
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
	}

	/*-
	 * cat /sys/kernel/debug/gpio
	 * Odroid C2 Hardware Technical details: http://www.hardkernel.com/main/products/prdt_info.php?g_code=G145457216438&tab_idx=2
	 * https://wiki.odroid.com/odroid-c2/hardware/hardware
	 * https://wiki.odroid.com/odroid-c2/hardware/expansion_connectors
	 * https://wiki.odroid.com/odroid-c2/application_note/gpio/enhancement_40pins#tab__odroid-c2
	 * https://wiki.odroid.com/odroid-c2/application_note/gpio/rpi.gpio
	 * PWM hardware details: http://odroid.com/dokuwiki/doku.php?id=en:c2_hardware_pwm
	 */
	public static class OdroidC2BoardInfo extends GenericLinuxArmBoardInfo {
		private static final int MEMORY = 2_048_000;
		private static final float ADC_VREF = 1.8f;

		private OdroidC2BoardInfo() {
			super(MAKE, Model.C2.toString(), Integer.valueOf(MEMORY),
					MAKE.toLowerCase() + "-" + Model.C2.toString().toLowerCase());
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new OdroidC2MmapGpio();
		}
		
		@Override
		public float getAdcVRef() {
			return ADC_VREF;
		}
	}
}
