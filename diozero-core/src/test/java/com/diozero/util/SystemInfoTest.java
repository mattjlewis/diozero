package com.diozero.util;

import org.junit.Assert;
import org.junit.Test;

import com.diozero.internal.board.beaglebone.BeagleBoneBoardInfoProvider;
import com.diozero.internal.board.chip.CHIPBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.internal.board.tinkerboard.TinkerBoardBoardInfoProvider;

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

@SuppressWarnings("static-method")
public class SystemInfoTest {
	@Test
	public void test() {
		//System.out.println(osReleaseProperties);
		//System.out.format("O/S Id='%s', Version='%s', Version Id='%s'%n",
		//		getOperatingSystemId(), getOperatingSystemVersion(), getOperatingSystemVersionId());
		String hardware;
		
		// CHIP
		validateBoard("Allwinner sun4i/sun5i Families", "0000", CHIPBoardInfoProvider.MAKE,
				CHIPBoardInfoProvider.MODEL_CHIP, 512);
		// BeagleBone Black
		validateBoard("Generic AM33XX (Flattened Device Tree)", "0000", BeagleBoneBoardInfoProvider.MAKE,
				BeagleBoneBoardInfoProvider.BeagleBoneBlackBoardInfo.MODEL, 512);
		// Asus Tinker Board
		validateBoard("Rockchip (Device Tree)", "0000", TinkerBoardBoardInfoProvider.MAKE,
				TinkerBoardBoardInfoProvider.TinkerBoardBoardInfo.MODEL, 2048);
		// Odroid C2
		validateBoard("ODROID-C2", "020b", OdroidBoardInfoProvider.MAKE,
				OdroidBoardInfoProvider.Model.C2.toString(), 2048);
		
		// Raspberry Pi
		hardware = "BCM2835";
		
		String line = "Revision        : a02082\n";
		validateBoard(hardware, line.split(":")[1].trim(), RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_3B, 1024);
		
		validateBoard(hardware, "0002", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0003", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0004", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0005", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0006", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 256);
		validateBoard(hardware, "0007", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_A, 256);
		validateBoard(hardware, "0008", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_A, 256);
		validateBoard(hardware, "0009", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_A, 256);
		validateBoard(hardware, "000d", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 512);
		validateBoard(hardware, "000e", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 512);
		validateBoard(hardware, "000f", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B, 512);
		validateBoard(hardware, "0010", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512);
		validateBoard(hardware, "0011", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE, 512);
		validateBoard(hardware, "0012", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 256);
		validateBoard(hardware, "0013", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512);
		validateBoard(hardware, "0014", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE, 512);
		validateBoard(hardware, "0015", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 256);
		// Pi2B, BCM2836, Sony, 1024MB
		validateBoard(hardware, "a01040", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_2B, 1024);
		// leonard - Pi2B, BCM2836, 1024MB
		validateBoard(hardware, "a01041", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_2B, 1024);
		// Pi2B, BCM2836, Embest, 1024MB
		validateBoard(hardware, "a21041", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_2B, 1024);
		// Pi2B, BCM2837, Embest, 1024MB
		validateBoard(hardware, "a22042", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_2B, 1024);
		// PiA+, BCM2835, Sony, 512
		validateBoard(hardware, "900021", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 512);
		// PiB+, BCM2835, Sony, 512
		validateBoard(hardware, "900032", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512);
		// matt, shirley - Pi Zero, BCM2835, Sony, 512MB
		validateBoard(hardware, "900092", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_ZERO, 512);
		// Pi Zero with DSI camera connector?
		validateBoard(hardware, "900093", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_ZERO, 512);
		// obi - Pi Zero W
		validateBoard(hardware, "9000c1", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_ZERO_W, 512);
		// stuart - Pi3B, BCM2837, Sony, 1024MB
		validateBoard(hardware, "a02082", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_3B, 1024);
		// PiCM3, BCM2837, Sony, 1024MB
		validateBoard(hardware, "a020a2", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3, 1024);
		// Pi3B, BCM2837, Embest, 1024MB
		validateBoard(hardware, "a22082", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_3B, 1024);
		// Pi3B, BCM2837, Sony Japan, 1024MB
		validateBoard(hardware, "a32082", RaspberryPiBoardInfoProvider.MAKE,
				RaspberryPiBoardInfoProvider.MODEL_3B, 1024);
	}
	
	private static void validateBoard(String hardware, String revision, String expectedMake, String expectedModel, int expectedMemory) {
		BoardInfo board_info = SystemInfo.lookupLocalBoardInfo(hardware, revision, null);
		System.out.println(revision + ": " + board_info);
		Assert.assertEquals(expectedMake, board_info.getMake());
		Assert.assertEquals(expectedModel, board_info.getModel());
		Assert.assertEquals(expectedMemory, board_info.getMemory());
	}
}
