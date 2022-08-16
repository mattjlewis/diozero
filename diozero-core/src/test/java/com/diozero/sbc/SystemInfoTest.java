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

import com.diozero.api.PinInfo;
import com.diozero.internal.board.allwinner.AllwinnerSun8iBoardInfoProvider;
import com.diozero.internal.board.beaglebone.BeagleBoneBoardInfoProvider;
import com.diozero.internal.board.chip.ChipBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider.PiBoardInfo;
import com.diozero.internal.board.tinkerboard.TinkerBoardBoardInfoProvider;

@SuppressWarnings("static-method")
public class SystemInfoTest {
	@Test
	public void test() {
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
	}

	@Test
	public void testPi() {
		PinInfo pin;

		PiBoardInfo board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0002",
				RaspberryPiBoardInfoProvider.MODEL_B, 256);
		pin = board_info.getByName("GPIO21");
		Assertions.assertEquals(13, pin.getPhysicalPin());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 13);
		Assertions.assertEquals(21, pin.getDeviceNumber());
		Assertions.assertEquals("GPIO21", pin.getName());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 3);
		Assertions.assertEquals("SDA1", pin.getName());
		Assertions.assertEquals(0, pin.getDeviceNumber());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 5);
		Assertions.assertEquals("SCL1", pin.getName());
		Assertions.assertEquals(1, pin.getDeviceNumber());

		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0003", RaspberryPiBoardInfoProvider.MODEL_B,
				256);
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 13);
		Assertions.assertEquals(21, pin.getDeviceNumber());
		Assertions.assertEquals("GPIO21", pin.getName());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 3);
		Assertions.assertEquals("SDA1", pin.getName());
		Assertions.assertEquals(0, pin.getDeviceNumber());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 5);
		Assertions.assertEquals("SCL1", pin.getName());
		Assertions.assertEquals(1, pin.getDeviceNumber());

		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0004", RaspberryPiBoardInfoProvider.MODEL_B,
				256);
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 13);
		Assertions.assertEquals(27, pin.getDeviceNumber());
		Assertions.assertEquals("GPIO27", pin.getName());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 3);
		Assertions.assertEquals("SDA1", pin.getName());
		Assertions.assertEquals(2, pin.getDeviceNumber());
		pin = board_info.getByPhysicalPinOrThrow(RaspberryPiBoardInfoProvider.DEFAULT_HEADER, 5);
		Assertions.assertEquals("SCL1", pin.getName());
		Assertions.assertEquals(3, pin.getDeviceNumber());

		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0005", RaspberryPiBoardInfoProvider.MODEL_B,
				256);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0006", RaspberryPiBoardInfoProvider.MODEL_B,
				256);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0007", RaspberryPiBoardInfoProvider.MODEL_A,
				256);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0008", RaspberryPiBoardInfoProvider.MODEL_A,
				256);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0009", RaspberryPiBoardInfoProvider.MODEL_A,
				256);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "000d", RaspberryPiBoardInfoProvider.MODEL_B,
				512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "000e", RaspberryPiBoardInfoProvider.MODEL_B,
				512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "000f", RaspberryPiBoardInfoProvider.MODEL_B,
				512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0010",
				RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0011",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE, 512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0012",
				RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 256);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0013",
				RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0014",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE, 512);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0015",
				RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 256);
		// Pi2B, BCM2836, Sony, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2836, "a01040",
				RaspberryPiBoardInfoProvider.MODEL_2B, 1_024_000);
		// leonard - Pi2B, BCM2836, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2836, "a01041",
				RaspberryPiBoardInfoProvider.MODEL_2B, 1_024_000);
		// Pi2B, BCM2836, Embest, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2836, "a21041",
				RaspberryPiBoardInfoProvider.MODEL_2B, 1_024_000);
		// Pi2B, BCM2837, Embest, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a22042",
				RaspberryPiBoardInfoProvider.MODEL_2B, 1_024_000);
		// PiA+, BCM2835, Sony, 512
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900021",
				RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 512_000);
		// PiB+, BCM2835, Sony, 512
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900032",
				RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512_000);
		// matt, shirley - Pi Zero, BCM2835, Sony, 512MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900092",
				RaspberryPiBoardInfoProvider.MODEL_ZERO, 512_000);
		// Pi Zero with DSI camera connector?
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900093",
				RaspberryPiBoardInfoProvider.MODEL_ZERO, 512_000);
		// obi - Pi Zero W
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "9000c1",
				RaspberryPiBoardInfoProvider.MODEL_ZERO_W, 512_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "9020e0",
				RaspberryPiBoardInfoProvider.MODEL_3A_PLUS, 512_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "920092",
				RaspberryPiBoardInfoProvider.MODEL_ZERO, 512_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "920093",
				RaspberryPiBoardInfoProvider.MODEL_ZERO, 512_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900061",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE, 512_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a020a0",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1_024_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02042",
				RaspberryPiBoardInfoProvider.MODEL_2B, 1_024_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a220a0",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1_024_000);
		// stuart - Pi3B, BCM2837, Sony, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837,
				"Revision        : a02082\n".split(":")[1].trim(), RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02082",
				RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);
		// PiCM3, BCM2837, Sony, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a020a2",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1_024_000);
		// Pi3B, BCM2837, Embest, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a22082",
				RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);
		// Pi3B, BCM2837, Sony Japan, 1024MB
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a32082",
				RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a52082",
				RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a22083",
				RaspberryPiBoardInfoProvider.MODEL_3B, 1_024_000);

		// CM3+
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02100",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3_PLUS, 1_024_000);

		// Pi3B+
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a020d3",
				RaspberryPiBoardInfoProvider.MODEL_3B_PLUS, 1_024_000);
		// Pi4B
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "a03111",
				RaspberryPiBoardInfoProvider.MODEL_4B, 1_024_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "b03111",
				RaspberryPiBoardInfoProvider.MODEL_4B, 2_048_000);
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03111",
				RaspberryPiBoardInfoProvider.MODEL_4B, 4_096_000);

		// Zero2W
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "903121",
				RaspberryPiBoardInfoProvider.MODEL_ZERO_2_W, 512_000);
		// 400
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03131",
				RaspberryPiBoardInfoProvider.MODEL_400, 4_096_000);
		// CM4
		board_info = validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03141",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_4, 4_096_000);
	}

	private static PiBoardInfo validatePiBoard(String processor, String revision, String expectedModel,
			int expectedMemory) {
		PiBoardInfo board_info = (PiBoardInfo) validateBoard(processor, revision, RaspberryPiBoardInfoProvider.MAKE,
				expectedModel, expectedMemory);
		Assertions.assertEquals(processor, board_info.getProcessor());
		return board_info;
	}

	private static BoardInfo validateBoard(String hardware, String revision, String expectedMake, String expectedModel,
			int expectedMemory) {
		return validateBoard(BoardInfo.UNKNOWN, hardware, revision, expectedMake, expectedModel, expectedMemory);
	}

	private static BoardInfo validateBoard(String model, String hardware, String revision, String expectedMake,
			String expectedModel, int expectedMemory) {
		LocalSystemInfo sys_info = new LocalSystemInfo(hardware, revision, model);
		BoardInfo board_info = LocalBoardInfoUtil.resolveLocalBoardInfo(sys_info);
		System.out.println(hardware + "/" + revision + ": " + board_info);
		Assertions.assertEquals(expectedMake, board_info.getMake());
		Assertions.assertEquals(expectedModel, board_info.getModel());
		if (expectedMemory != -1) {
			Assertions.assertEquals(expectedMemory, board_info.getMemoryKb());
		}

		board_info.populateBoardPinInfo();

		return board_info;
	}
}
