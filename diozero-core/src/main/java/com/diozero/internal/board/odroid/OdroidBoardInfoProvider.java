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

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.MmapGpioInterface;
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
			if (hardware.equals(C2_HARDWARE_ID) || hardware.endsWith(C2_HARDWARE_ID)) {
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
		public void initialisePins() {
			// TODO Add pins when support for this board is added
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
	}

	/**
	 * <p>See <a href="http://www.hardkernel.com/main/products/prdt_info.php?g_code=G145457216438&tab_idx=2">Odroid C2 Hardware Technical details</a>.</p>
	 * https://wiki.odroid.com/odroid-c2/hardware/hardware
	 * https://wiki.odroid.com/odroid-c2/hardware/expansion_connectors
	 * https://wiki.odroid.com/odroid-c2/application_note/gpio/enhancement_40pins#tab__odroid-c2
	 * https://wiki.odroid.com/odroid-c2/application_note/gpio/rpi.gpio
	 * <p>Also see <a href="http://odroid.com/dokuwiki/doku.php?id=en:c2_hardware_pwm">Hardware PWM</a> for details on PWM.</p>
	 */
	public static class OdroidC2BoardInfo extends BoardInfo {
		private static final int MEMORY = 2048;
		
		private OdroidC2BoardInfo() {
			super(MAKE, Model.C2.toString(), MEMORY, MAKE.toLowerCase() + "-" + Model.C2.toString().toLowerCase());
		}
		
		@Override
		public void initialisePins() {
			// cat /sys/kernel/debug/gpio
			
			// FIXME Externalise this to a file
			
			// J2 Header
			/*-
			 * 3.14 Kernel from HardKernel
			addGpioPinInfo(205, 3, PinInfo.DIGITAL_IN_OUT);			// I2C1-SDA - 447
			addGpioPinInfo(206, 5, PinInfo.DIGITAL_IN_OUT);			// I2C1-SCL - 448
			addGpioPinInfo(207, 27, PinInfo.DIGITAL_IN_OUT);			// I2C2-SDA - 449
			addGpioPinInfo(77, 28, PinInfo.DIGITAL_IN_OUT);			// I2C2-SCL - 450
			addGpioPinInfo(214, 35, PinInfo.DIGITAL_IN_OUT);			// GPIOY.BIT3 - 456
			addGpioPinInfo(218, 36, PinInfo.DIGITAL_IN_OUT);			// GPIOY.BIT7 - 460
			addGpioPinInfo(219, 31, PinInfo.DIGITAL_IN_OUT);			// GPIOY.BIT8 - 461
			addGpioPinInfo(224, 32, PinInfo.DIGITAL_IN_OUT);			// GPIOY.BIT13 - 466
			addGpioPinInfo(225, 26, PinInfo.DIGITAL_IN_OUT);			// GPIOY.BIT14 - 467
			addGpioPinInfo(228, 29, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT10 - 470
			addGpioPinInfo(229, 24, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT1 - 471
			addGpioPinInfo(230, 23, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT2 - 472
			addGpioPinInfo(231, 22, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT3 - 473
			addGpioPinInfo(232, 21, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT4 - 474
			addGpioPinInfo(233, 18, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT5 - 475
			addPwmPinInfo(234, 33, 0, PinInfo.DIGITAL_IN_OUT_PWM);	// GPIOX.BIT6 - 476
			addPwmPinInfo(235, 19, 1, PinInfo.DIGITAL_IN_OUT_PWM);	// GPIOX.BIT7 - 477
			addGpioPinInfo(236, 16, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT8 - 478
			addGpioPinInfo(237, 15, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT9 - 479
			addGpioPinInfo(238, 12, PinInfo.DIGITAL_IN_OUT);			// GPIOY.BIT10 - 480
			addGpioPinInfo(239, 13, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT11 - 481
			addGpioPinInfo(113, 8, PinInfo.DIGITAL_IN_OUT);			// UART TX - 482
			addGpioPinInfo(114, 10, PinInfo.DIGITAL_IN_OUT);			// UART RX - 483
			addGpioPinInfo(247, 11, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT19 - 489
			addGpioPinInfo(249, 7, PinInfo.DIGITAL_IN_OUT);			// GPIOX.BIT21 - 491
			 */
			// Armbian 5.x Kernel
			addGpioPinInfo(447, "I2C A SDA", 3, PinInfo.DIGITAL_IN_OUT, 1, 69);			// I2C1-SDA - 205
			addGpioPinInfo(448, "I2C A SCK", 5, PinInfo.DIGITAL_IN_OUT, 1, 70);			// I2C1-SCL - 206
			addGpioPinInfo(449, "I2C B SDA", 27, PinInfo.DIGITAL_IN_OUT, 1, 71);			// I2C2-SDA - 207
			addGpioPinInfo(450, "I2C B SCK", 28, PinInfo.DIGITAL_IN_OUT, 1, 72);			// I2C2-SCL - 77
			addGpioPinInfo(456, "J2 Header Pin35", 35, PinInfo.DIGITAL_IN_OUT, 1, 78);	// GPIOY.BIT3 - 214
			addGpioPinInfo(460, "J2 Header Pin36", 36, PinInfo.DIGITAL_IN_OUT, 1, 82);	// GPIOY.BIT7 - 218
			addGpioPinInfo(461, "J2 Header Pin31", 31, PinInfo.DIGITAL_IN_OUT, 1, 83);	// GPIOY.BIT8 - 219
			addGpioPinInfo(466, "J2 Header Pin32", 32, PinInfo.DIGITAL_IN_OUT, 1, 88);	// GPIOY.BIT13 - 224
			addGpioPinInfo(467, "J2 Header Pin26", 26, PinInfo.DIGITAL_IN_OUT, 1, 89);	// GPIOY.BIT14 - 225
			addGpioPinInfo(470, "J2 Header Pin29", 29, PinInfo.DIGITAL_IN_OUT, 1, 92);	// GPIOX.BIT10 - 228
			addGpioPinInfo(471, "J2 Header Pin24", 24, PinInfo.DIGITAL_IN_OUT, 1, 93);	// GPIOX.BIT1 - 229
			addGpioPinInfo(472, "J2 Header Pin23", 23, PinInfo.DIGITAL_IN_OUT, 1, 94);	// GPIOX.BIT2 - 230
			addGpioPinInfo(473, "J2 Header Pin22", 22, PinInfo.DIGITAL_IN_OUT, 1, 95);	// GPIOX.BIT3 - 231
			addGpioPinInfo(474, "J2 Header Pin21", 21, PinInfo.DIGITAL_IN_OUT, 1, 96);	// GPIOX.BIT4 - 232
			addGpioPinInfo(475, "J2 Header Pin18", 18, PinInfo.DIGITAL_IN_OUT, 1, 97);	// GPIOX.BIT5 - 233
			addPwmPinInfo(476, "J2 Header Pin33", 33, 0, PinInfo.DIGITAL_IN_OUT_PWM, 1, 98);	// GPIOX.BIT6 - 234
			addPwmPinInfo(477, "J2 Header Pin19", 19, 1, PinInfo.DIGITAL_IN_OUT_PWM, 1, 99);	// GPIOX.BIT7 - 235
			addGpioPinInfo(478, "J2 Header Pin16", 16, PinInfo.DIGITAL_IN_OUT, 1, 100);	// GPIOX.BIT8 - 236
			addGpioPinInfo(479, "J2 Header Pin15", 15, PinInfo.DIGITAL_IN_OUT, 1, 101);	// GPIOX.BIT9 - 237
			addGpioPinInfo(480, "J2 Header Pin12", 12, PinInfo.DIGITAL_IN_OUT, 1, 102);	// GPIOY.BIT10 - 238
			addGpioPinInfo(481, "J2 Header Pin13", 13, PinInfo.DIGITAL_IN_OUT, 1, 103);	// GPIOX.BIT11 - 239
			addGpioPinInfo(482, "J2 Header Pin8", 8, PinInfo.DIGITAL_IN_OUT, 1, 104);	// UART TX - 113
			addGpioPinInfo(483, "J2 Header Pin10", 10, PinInfo.DIGITAL_IN_OUT, 1, 105);	// UART RX - 114
			addGpioPinInfo(489, "J2 Header Pin11", 11, PinInfo.DIGITAL_IN_OUT, 1, 111);	// GPIOX.BIT19 - 247
			addGpioPinInfo(491, "J2 Header Pin7", 7, PinInfo.DIGITAL_IN_OUT, 1, 113);	// GPIOX.BIT21 - 249
			
			addGeneralPinInfo(1, PinInfo.VCC_3V3);
			addGeneralPinInfo(2, PinInfo.VCC_5V);
			addGeneralPinInfo(4, PinInfo.VCC_5V);
			addGeneralPinInfo(6, PinInfo.GROUND);
			addGeneralPinInfo(9, PinInfo.GROUND);
			addGeneralPinInfo(14, PinInfo.GROUND);
			addGeneralPinInfo(17, PinInfo.VCC_3V3);
			addGeneralPinInfo(20, PinInfo.GROUND);
			addGeneralPinInfo(25, PinInfo.GROUND);
			addGeneralPinInfo(30, PinInfo.GROUND);
			addGeneralPinInfo(34, PinInfo.GROUND);
			addAdcPinInfo(1, 37);	// ADC.AIN1
			addAdcPinInfo(0, 40);	// ADC.AIN0
			addGeneralPinInfo(38, PinInfo.VCC_1V8);
			addGeneralPinInfo(39, PinInfo.GROUND);
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
