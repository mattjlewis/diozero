package com.diozero.internal.board.tinkerboard;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     TinkerBoardBoardInfoProvider.java  
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

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.MmapGpioInterface;
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
		//private static final String LIBRARY_PATH = "linux-arm";

		private TinkerBoardBoardInfo() {
			super(MAKE, MODEL, MEMORY, LIBRARY_PATH);
		}

		@Override
		public void initialisePins() {
			// FIXME Externalise this to a file

			// Source: https://tinkerboarding.co.uk/wiki/index.php/GPIO

			// GPIO0_C1 - gpiochip0 (24 lines, GPIOs start at 0)
			int chip = 0;
			int lines = 24;
			int line_start = 0;
			int line_offset = 17; // Line 0:17 (CLKOUT) - GPIO #17
			addGpioPinInfo(line_start + line_offset, "GPIO0_C1", 7, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_start += lines;

			// gpiochip1 (32 lines, GPIOs start at 0+24=24)
			chip++;
			lines = 32;
			// Add lines
			line_start += lines;
			
			// gpiochip2 (32 lines, GPIOs start at 24+32=56)
			chip++;
			lines = 32;
			// Add lines
			line_start += lines;
			
			// gpiochip3 (32 lines, GPIOs start at 56+32=88)
			chip++;
			lines = 32;
			// Add lines
			line_start += lines;
			
			// gpiochip4 (32 lines, GPIOs start at 88+32=120)
			chip++;
			lines = 32;
			// Add lines
			line_start += lines;

			// GPIO5B (GP5B0-GP5B7) - gpiochip5 (32 lines, GPIOs start at 120+32=152)
			chip++;
			lines = 32;
			line_offset = 8; // Line 5:8 (UART-1 RX) - GPIO #160
			addGpioPinInfo(line_start + line_offset, "GPIO5_B0", 10, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 9; // Line 5:9 (UART-1 TX) - GPIO #161
			addGpioPinInfo(line_start + line_offset, "GPIO5_B1", 8, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 10; // Line 5:10 (UART-1 CTSN) - GPIO #162
			addGpioPinInfo(line_start + line_offset, "GPIO5_B2", 16, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 11; // Line 5:11 (UART-1 RTSN) - GPIO #163
			addGpioPinInfo(line_start + line_offset, "GPIO5_B3", 18, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 12; // Line 5:12 (SPI-0 CLK) - GPIO #164
			addGpioPinInfo(line_start + line_offset, "GPIO5_B4", 11, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 13; // Line 5:13 (SPI-0 CS N0) - GPIO #165
			addGpioPinInfo(line_start + line_offset, "GPIO5_B5", 29, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 14; // Line 5:14 (SPI-0 TX) - GPIO #166
			addGpioPinInfo(line_start + line_offset, "GPIO5_B6", 13, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 15; // Line 5:15 (SPI-0 RX) - GPIO #167
			addGpioPinInfo(line_start + line_offset, "GPIO5_B7", 15, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			// GPIO5C (GP5C0-GP5C3) - gpiochip5
			line_offset = 16; // Line 5:16 (SPI-0 CS N1) - GPIO #168
			addGpioPinInfo(line_start + line_offset, "GPIO5_C0", 31, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 17; // Line 5:17 (Not connected)
			addGpioPinInfo(line_start + line_offset, "GPIO5_C1", -1, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 18; // Line 5:18 (Not connected)
			addGpioPinInfo(line_start + line_offset, "GPIO5_C2", -1, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 19; // Line 5:19 - GPIO #171
			addGpioPinInfo(line_start + line_offset, "GPIO5_C3", 22, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_start += lines;

			// GPIO6A (GP6A0-GP6A1) - gpiochip6 (32 lines, GPIOs start at 152+32=184)
			chip++;
			lines = 32;
			line_offset = 0; // Line 6:0 (PCM / I2C CLK) - GPIO #184
			addGpioPinInfo(line_start + line_offset, "GPIO6_A0", 12, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 1; // Line 6:1 (PCM I2S FS) - GPIO #185
			addGpioPinInfo(line_start + line_offset, "GPIO6_A1", 35, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			// GPIO6A (GP6A3-GP6A4) - gpiochip6
			line_offset = 3; // Line 6:3 (I2S SDI) - GPIO #187
			addGpioPinInfo(line_start + line_offset, "GPIO6_A3", 38, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 4; // Line 6:4 (I2S SDO) - GPIO #188
			addGpioPinInfo(line_start + line_offset, "GPIO6_A4", 40, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_start += lines;

			// GPIO7A (GP7A7) - gpiochip7 (32 lines, GPIOs start at 184+32=216)
			chip++;
			lines = 32;
			line_offset = 7; // Line 7:7 (UART-3 RX) - GPIO #223
			addGpioPinInfo(line_start + line_offset, "GPIO7_A7", 36, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			// GPIO7B (GP7B0-GP7B2) - gpiochip7
			line_offset = 8; // Line 7:8 (UART-3 TX) - GPIO #224
			addGpioPinInfo(line_start + line_offset, "GPIO7_B0", 37, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 9; // Line 7:9 (Not connected)
			addGpioPinInfo(line_start + line_offset, "GPIO7_B1", -1, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 10; // Line 7:10 (Not connected)
			addGpioPinInfo(line_start + line_offset, "GPIO7_B2", -1, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			// GPIO7CL (GP7C1-GP7C2) - gpiochip7
			line_offset = 17; // Line 7:17 (I2C-4 SDA) - GPIO #233
			addGpioPinInfo(line_start + line_offset, "GPIO7_C1", 27, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 18; // Line 7:18 (I2C-4 SCL) - GPIO #234
			addGpioPinInfo(line_start + line_offset, "GPIO7_C2", 28, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			// GPIO7CH (GP7C6-GP7C7) - gpiochip7
			line_offset = 22; // Line 7:22 (UART-2 RX) - GPIO #238
			addPwmPinInfo(line_start + line_offset, "GPIO7_C6", 33, 0, PinInfo.DIGITAL_IN_OUT_PWM, chip, line_offset);
			line_offset = 23; // Line 7:23 (UART-2 TX) - GPIO #239
			addPwmPinInfo(line_start + line_offset, "GPIO7_C7", 32, 1, PinInfo.DIGITAL_IN_OUT_PWM, chip, line_offset);
			line_start += lines;

			// GPIO8A (GP8A3-GP8A7) - gpiochip8 (16 lines, GPIOs start at 216+32=248)
			chip++;
			lines = 16;
			line_offset = 3; // Line 8:3 (SPI-2 CS N1) - GPIO #251
			addGpioPinInfo(line_start + line_offset, "GPIO8_A3", 26, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 4; // Line 8:4 (I2C-1 SDA) - GPIO #252
			addGpioPinInfo(line_start + line_offset, "GPIO8_A4", 3, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 5; // Line 8:5 (I2C-1 SCL) - GPIO #253
			addGpioPinInfo(line_start + line_offset, "GPIO8_A5", 5, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 6; // Line 8:6 (SPI-2 CLK) - GPIO #254
			addGpioPinInfo(line_start + line_offset, "GPIO8_A6", 23, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 7; // Line 8:7 (SPI-2 CS N0) - GPIO #255
			addGpioPinInfo(line_start + line_offset, "GPIO8_A7", 24, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			// GPIO8B (GP8B0-GP8B1) - gpiochip8
			line_offset = 8; // Line 8:8 (SPI-2 RX) - GPIO #256
			addGpioPinInfo(line_start + line_offset, "GPIO8_B0", 21, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_offset = 9; // Line 8:9 (SPI-2 TX) - GPIO #257
			addGpioPinInfo(line_start + line_offset, "GPIO8_B1", 19, PinInfo.DIGITAL_IN_OUT, chip, line_offset);
			line_start += lines;

			// Add non-GPIO pins
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
			addGeneralPinInfo(39, PinInfo.GROUND);
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
