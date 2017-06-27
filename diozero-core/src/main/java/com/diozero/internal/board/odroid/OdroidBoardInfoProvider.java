package com.diozero.internal.board.odroid;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     OdroidBoardInfoProvider.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


import java.util.HashMap;
import java.util.Map;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.mmap.MmapGpioInterface;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

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
		//BOARDS.put("????", new OdroidBoardInfo(Model.C0, 1024));
		//BOARDS.put("0000", new OdroidBoardInfo(Model.U2_U3, 2048));
		BOARD_REVISIONS.put("000a", ODROID_C1);
		//BOARDS.put("0100", new OdroidBoardInfo(Model.XU_3_4, 2048));
		BOARD_REVISIONS.put("020b", ODROID_C2);
	}

	@Override
	public BoardInfo lookup(String hardware, String revision, Integer memoryKb) {
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

			// TODO Add pins when support for this board is added
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
	}

	/**
	 * <p>See <a href="http://www.hardkernel.com/main/products/prdt_info.php?g_code=G145457216438&tab_idx=2">Odroid C2 Hardware Technical details</a>.</p>
	 * <p>Also see <a href="http://odroid.com/dokuwiki/doku.php?id=en:c2_hardware_pwm">Hardware PWM</a> for details on PWM.</p>
	 */
	public static class OdroidC2BoardInfo extends BoardInfo {
		private static final int MEMORY = 2048;
		
		private OdroidC2BoardInfo() {
			super(MAKE, Model.C2.toString(), MEMORY, MAKE.toLowerCase() + "/" + Model.C2.toString().toLowerCase());

			// 3V3 1 | 2 5V0
			addGpioPinInfo(205, 3, PinInfo.DIGITAL_IN_OUT);			// I2C1-SDA
			// 4 5V0
			addGpioPinInfo(206, 5, PinInfo.DIGITAL_IN_OUT);			// I2C1-SCL
			// 6 GND
			addGpioPinInfo(249, 7, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT21
			addGpioPinInfo(113, 8, PinInfo.DIGITAL_IN_OUT);			// UART TX
			// 9 GND
			addGpioPinInfo(114, 10, PinInfo.DIGITAL_IN_OUT);		// UART RX
			addGpioPinInfo(247, 11, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT19
			addGpioPinInfo(238, 12, PinInfo.DIGITAL_IN_OUT);		// GPIOY.BIT10
			addGpioPinInfo(239, 13, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT11
			// 14 GND
			addGpioPinInfo(237, 15, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT9
			addGpioPinInfo(236, 16, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT8
			// 17 3V3
			addGpioPinInfo(233, 18, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT5
			addPwmPinInfo(235, 19, 1, PinInfo.DIGITAL_IN_OUT_PWM);	// GPIOX.BIT7
			// 20 GND
			addGpioPinInfo(232, 21, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT4
			addGpioPinInfo(231, 22, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT3
			addGpioPinInfo(230, 23, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT2
			addGpioPinInfo(229, 24, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT1
			// 25 GND
			addGpioPinInfo(225, 26, PinInfo.DIGITAL_IN_OUT);		// GPIOY.BIT14
			addGpioPinInfo(207, 27, PinInfo.DIGITAL_IN_OUT);		// I2C2-SDA
			addGpioPinInfo(77, 28, PinInfo.DIGITAL_IN_OUT);			// I2C2-SCL
			addGpioPinInfo(228, 29, PinInfo.DIGITAL_IN_OUT);		// GPIOX.BIT10
			// 30 GND
			addGpioPinInfo(219, 31, PinInfo.DIGITAL_IN_OUT);		// GPIOY.BIT8
			addGpioPinInfo(224, 32, PinInfo.DIGITAL_IN_OUT);		// GPIOY.BIT13
			addPwmPinInfo(234, 33, 0, PinInfo.DIGITAL_IN_OUT_PWM);	// GPIOX.BIT6
			// 34 GND
			addGpioPinInfo(214, 35, PinInfo.DIGITAL_IN_OUT);		// GPIOY.BIT3
			addGpioPinInfo(218, 36, PinInfo.DIGITAL_IN_OUT);		// GPIOY.BIT7
			addAdcPinInfo(1, 37);									// ADC.AIN1
			// 38 1V8
			// 39 GND
			addAdcPinInfo(0, 40);									// ADC.AIN0
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
		
		@Override
		public MmapGpioInterface createMmapGpio() {
			return new OdroidC2MmapGpio();
		}
	}
}
