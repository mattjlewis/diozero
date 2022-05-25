package com.diozero.sbc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SystemInfoTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diozero.internal.board.allwinner.AllwinnerSun8iBoardInfoProvider;
import com.diozero.internal.board.beaglebone.BeagleBoneBoardInfoProvider;
import com.diozero.internal.board.chip.ChipBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.internal.board.tinkerboard.TinkerBoardBoardInfoProvider;

@SuppressWarnings("static-method")
public class SystemInfoTest {
	@Test
	public void test() {
		// System.out.println(osReleaseProperties);
		// System.out.format("O/S Id='%s', Version='%s', Version Id='%s'%n",
		// getOperatingSystemId(), getOperatingSystemVersion(),
		// getOperatingSystemVersionId());

		// CHIP
		validateBoard("Allwinner sun4i/sun5i Families", "0000", ChipBoardInfoProvider.MAKE,
				ChipBoardInfoProvider.MODEL_CHIP, 512_000);
		// NanoPi Duo2
		validateBoard("FriendlyElec NanoPi-Duo2", "Allwinner sun8i Family", "0000",
				AllwinnerSun8iBoardInfoProvider.MAKE, "FriendlyElec NanoPi-Duo2", -1);
		// BeagleBone Black
		validateBoard("TI AM335x BeagleBone Black", "Generic AM33XX (Flattened Device Tree)", "0000",
				BeagleBoneBoardInfoProvider.MAKE, "Black", 512_000);
		// BeagleBone Green
		validateBoard("TI AM335x BeagleBone Green", "Generic AM33XX (Flattened Device Tree)", "0000",
				BeagleBoneBoardInfoProvider.MAKE, "Green", 512_000);
		// Asus Tinker Board
		validateBoard("Rockchip RK3288 Asus Tinker Board", "Rockchip (Device Tree)", "0000",
				TinkerBoardBoardInfoProvider.MAKE, TinkerBoardBoardInfoProvider.TinkerBoardBoardInfo.MODEL, 2_048_000);
		// Odroid C2
		validateBoard("Hardkernel ODROID-C2", "ODROID-C2", "020b", OdroidBoardInfoProvider.MAKE,
				OdroidBoardInfoProvider.Model.C2.toString(), 2_048_000);

		// Raspberry Pi
		String hardware = "BCM2835";

		String line = "Revision        : a02082\n";
		validateBoard(hardware, line.split(":")[1].trim(), RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);

		validateBoard(hardware, "0002", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0003", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0004", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0005", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0006", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0007", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_A, 256);
		validateBoard(hardware, "0008", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_A, 256);
		validateBoard(hardware, "0009", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_A, 256);
		validateBoard(hardware, "000d", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 512);
		validateBoard(hardware, "000e", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 512);
		validateBoard(hardware, "000f", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B, 512);
		validateBoard(hardware, "0010", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B_PLUS,
				512);
		validateBoard(hardware, "0011", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.COMPUTE_MODULE,
				512);
		validateBoard(hardware, "0012", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_A_PLUS,
				256);
		validateBoard(hardware, "0013", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B_PLUS,
				512);
		validateBoard(hardware, "0014", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.COMPUTE_MODULE,
				512);
		validateBoard(hardware, "0015", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_A_PLUS,
				256);
		// Pi2B, BCM2836, Sony, 1024MB
		validateBoard(hardware, "a01040", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_2B,
				1_024_000);
		// leonard - Pi2B, BCM2836, 1024MB
		validateBoard(hardware, "a01041", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_2B,
				1_024_000);
		// Pi2B, BCM2836, Embest, 1024MB
		validateBoard(hardware, "a21041", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_2B,
				1_024_000);
		// Pi2B, BCM2837, Embest, 1024MB
		validateBoard(hardware, "a22042", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_2B,
				1_024_000);
		// PiA+, BCM2835, Sony, 512
		validateBoard(hardware, "900021", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_A_PLUS,
				512_000);
		// PiB+, BCM2835, Sony, 512
		validateBoard(hardware, "900032", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_B_PLUS,
				512_000);
		// matt, shirley - Pi Zero, BCM2835, Sony, 512MB
		validateBoard(hardware, "900092", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_ZERO,
				512_000);
		// Pi Zero with DSI camera connector?
		validateBoard(hardware, "900093", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_ZERO,
				512_000);
		// obi - Pi Zero W
		validateBoard(hardware, "9000c1", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_ZERO_W,
				512_000);
		validateBoard(hardware, "9020e0", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3A_PLUS,
				512_000);
		validateBoard(hardware, "920092", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_ZERO,
				512_000);
		validateBoard(hardware, "920093", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_ZERO,
				512_000);
		validateBoard(hardware, "900061", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE, 512_000);
		validateBoard(hardware, "a020a0", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1_024_000);
		validateBoard(hardware, "a02042", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_2B,
				1_024_000);
		validateBoard(hardware, "a220a0", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1_024_000);
		// stuart - Pi3B, BCM2837, Sony, 1024MB
		validateBoard(hardware, "a02082", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3B,
				1_024_000);
		// PiCM3, BCM2837, Sony, 1024MB
		validateBoard(hardware, "a020a2", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1_024_000);
		// Pi3B, BCM2837, Embest, 1024MB
		validateBoard(hardware, "a22082", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3B,
				1_024_000);
		// Pi3B, BCM2837, Sony Japan, 1024MB
		validateBoard(hardware, "a32082", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3B,
				1_024_000);
		validateBoard(hardware, "a52082", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3B,
				1_024_000);
		validateBoard(hardware, "a22083", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3B,
				1_024_000);

		// CM3+
		validateBoard(hardware, "a02100", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3_PLUS, 1_024_000);

		// Pi3B+
		validateBoard(hardware, "a020d3", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_3B_PLUS,
				1_024_000);
		// Pi4B
		validateBoard(RaspberryPiBoardInfoProvider.MAKE + RaspberryPiBoardInfoProvider.MODEL_4B, hardware, "a03111",
				RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_4B, 1_024_000);
		validateBoard(hardware, "b03111", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_4B,
				2_048_000);
		validateBoard(hardware, "c03111", RaspberryPiBoardInfoProvider.MAKE, RaspberryPiBoardInfoProvider.MODEL_4B,
				4_096_000);
	}

	private static void validateBoard(String hardware, String revision, String expectedMake, String expectedModel,
			int expectedMemory) {
		validateBoard(BoardInfo.UNKNOWN, hardware, revision, expectedMake, expectedModel, expectedMemory);
	}

	private static void validateBoard(String model, String hardware, String revision, String expectedMake,
			String expectedModel, int expectedMemory) {
		LocalSystemInfo sys_info = new LocalSystemInfo(hardware, revision, model);
		BoardInfo board_info = LocalBoardInfoUtil.resolveLocalBoardInfo(sys_info);
		System.out.println(hardware + "/" + revision + ": " + board_info);
		Assertions.assertEquals(expectedMake, board_info.getMake());
		Assertions.assertEquals(expectedModel, board_info.getModel());
		if (expectedMemory != -1) {
			Assertions.assertEquals(expectedMemory, board_info.getMemoryKb());
		}
	}
}
