package com.diozero.sbc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SystemInfoTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider.PiBoardInfo;

@SuppressWarnings("static-method")
public class SystemInfoTest {
	private static final int _1GB = 1_024_000;

	@Test
	public void test() {
		// Asus Tinker Board
		validateLinuxArmBoard(List.of("asus,rk3288-tinker", "rockchip,rk3288"), "Rockchip RK3288 Asus Tinker Board",
				"0000", 2 * _1GB,
				List.of(new PinInfo("GPIO", "DEFAULT", 160, 10, "GPIO5_B0", PinInfo.DIGITAL_IN_OUT, 160, 5, 8),
						new PinInfo("GPIO", "DEFAULT", 233, 27, "GPIO7_C1", PinInfo.DIGITAL_IN_OUT, 233, 7, 17),
						new PinInfo("PWM", "DEFAULT", 239, 32, 0, 1, "GPIO7_C7", PinInfo.DIGITAL_IN_OUT_PWM, 239, 7, 23,
								PinInfo.NOT_DEFINED)));
		// BeagleBone Black
		validateLinuxArmBoard(List.of("ti,am335x-bone-green", "ti,am335x-bone-black", "ti,am335x-bone", "ti,am33xx"),
				"TI AM335x BeagleBone Black", "0000", _1GB / 2,
				List.of(new PinInfo("GPIO", "P8", 26, 14, "GPMC_AD10", PinInfo.DIGITAL_IN_OUT, 26, 0, 26)));
		// BeagleBone Green
		validateLinuxArmBoard(List.of("ti,am335x-bone-green", "ti,am335x-bone-black", "ti,am335x-bone", "ti,am33xx"),
				"TI AM335x BeagleBone Green", "0000", _1GB / 2,
				List.of(new PinInfo("GPIO", "P8", 26, 14, "GPMC_AD10", PinInfo.DIGITAL_IN_OUT, 26, 0, 26)));
		// Hardkernel Odroid C1
		validateLinuxArmBoard(List.of("hardkernel,odroid-c1", "amlogic,meson8b"), "Hardkernel ODROID-C1", "", _1GB,
				Collections.emptyList());
		// Hardkernel Odroid C2
		validateLinuxArmBoard(List.of("hardkernel,odroid-c2", "amlogic,meson-gxbb"), "Hardkernel ODROID-C2", "020b",
				2 * _1GB,
				List.of(new PinInfo("GPIO", "DEFAULT", 456, 35, "J2 Header Pin35", PinInfo.DIGITAL_IN_OUT, 456, 1, 78),
						new PinInfo("PWM", "DEFAULT", 477, 19, 0, 1, "J2 Header Pin19", PinInfo.DIGITAL_IN_OUT_PWM, 477,
								1, 99, PinInfo.NOT_DEFINED)));
		// Hardkernel Odroid N2+
		validateLinuxArmBoard(List.of("hardkernel,odroid-n2-plus", "amlogic,g12b"), "Hardkernel ODROID-N2Plus", "400",
				4 * _1GB,
				List.of(new PinInfo("GPIO", "DEFAULT", 488, 8, "GPIOX.12", PinInfo.DIGITAL_IN_OUT, 488, 1, 78),
						new PinInfo("PWM", "DEFAULT", 481, 33, 4, 0, "PWM_C/GPIOX.5", PinInfo.DIGITAL_IN_OUT_PWM, 481,
								1, 71, PinInfo.NOT_DEFINED)));
		// NextThing CHIP
		validateLinuxArmBoard(List.of("nextthing,chip", "allwinner,sun5i-a13"), "NextThing C.H.I.P.", "0000", _1GB / 2,
				List.of(new PinInfo("GPIO", "U13", 98, 17, "LCD-D2", PinInfo.DIGITAL_IN_OUT, 98, PinInfo.NOT_DEFINED,
						PinInfo.NOT_DEFINED),
						new PinInfo("PWM", "U13", 34, 18, 0, 0, "PWM0", PinInfo.DIGITAL_IN_OUT_PWM, 34,
								PinInfo.NOT_DEFINED, PinInfo.NOT_DEFINED, PinInfo.NOT_DEFINED)));
		// Nanopi Neo
		validateLinuxArmBoard(List.of("friendlyarm,nanopi-neo", "allwinner,sun8i-h3"), "FriendlyARM NanoPi NEO", "0000",
				-1,
				List.of(new PinInfo("GPIO", "RHS", 2, 13, "PA2", PinInfo.DIGITAL_IN_OUT, 2, 0, 2), new PinInfo("PWM",
						"RHS", 6, 12, 0, 1, "PWM1", PinInfo.DIGITAL_IN_OUT_PWM, 6, 0, 6, PinInfo.NOT_DEFINED)));
		// Nanopi Duo2
		validateLinuxArmBoard(List.of("friendlyarm,nanopi-duo2", "allwinner,sun8i-h3"), "FriendlyElec NanoPi-Duo2",
				"0000", -1,
				List.of(new PinInfo("GPIO", "DEFAULT", 363, 9, "PL11", PinInfo.DIGITAL_IN_OUT, 363, 1, 11),
						new PinInfo("PWM", "DEFAULT", 5, 2, 0, 0, "PWM0", PinInfo.DIGITAL_IN_OUT_PWM, 5, 0, 5,
								PinInfo.NOT_DEFINED)));
		// Orange Pi Zero Plus
		validateLinuxArmBoard(List.of("xunlong,orangepi-zero-plus", "allwinner,sun50i-h5"),
				"Xunlong Orange Pi Zero Plus", "", -1,
				List.of(new PinInfo("GPIO", "DEFAULT", 7, 12, "PA7", PinInfo.DIGITAL_IN_OUT, 7, 1, 7),
						new PinInfo("PWM", "DEFAULT", 6, 7, 0, 1, "PWM1", PinInfo.DIGITAL_IN_OUT_PWM, 6, 1, 6,
								PinInfo.NOT_DEFINED)));
		// Orange Pi One Plus
		validateLinuxArmBoard(List.of("xunlong,orangepi-one-plus", "allwinner,sun50i-h6"), "OrangePi One Plus", "", -1,
				Collections.emptyList());
		// Orange Pi 3 LTS
		validateLinuxArmBoard(List.of("xunlong,orangepi-3-lts", "allwinner,sun50i-h6"), "OrangePi 3 LTS", "", -1,
				Collections.emptyList());
		// Radxa ROCK 4C+
		validateLinuxArmBoard(List.of("radxa,rock-4c-plus", "rockchip,rk3399"), "Radxa ROCK 4C+", "", 4 * _1GB,
				Collections.emptyList());
	}

	@Test
	public void testPi() {
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0002", RaspberryPiBoardInfoProvider.MODEL_B, 256,
				List.of(new PinInfo("GPIO", "J8", 21, 13, "GPIO21", PinInfo.DIGITAL_IN_OUT, 21, 0, 21),
						new PinInfo("GPIO", "J8", 0, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 0, 0, 0),
						new PinInfo("GPIO", "J8", 1, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 1, 0, 1)),
				RaspberryPiBoardInfoProvider.EGOMAN, "1.0");

		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0003", RaspberryPiBoardInfoProvider.MODEL_B, 256,
				List.of(new PinInfo("GPIO", "J8", 21, 13, "GPIO21", PinInfo.DIGITAL_IN_OUT, 21, 0, 21),
						new PinInfo("GPIO", "J8", 0, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 0, 0, 0),
						new PinInfo("GPIO", "J8", 1, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 1, 0, 1)),
				RaspberryPiBoardInfoProvider.EGOMAN, "1.1");

		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0004", RaspberryPiBoardInfoProvider.MODEL_B, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.SONY, "2.0");

		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0005", RaspberryPiBoardInfoProvider.MODEL_B, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.QISDA, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0006", RaspberryPiBoardInfoProvider.MODEL_B, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.EGOMAN, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0007", RaspberryPiBoardInfoProvider.MODEL_A, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.EGOMAN, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0008", RaspberryPiBoardInfoProvider.MODEL_A, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.SONY, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0009", RaspberryPiBoardInfoProvider.MODEL_A, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.QISDA, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "000d", RaspberryPiBoardInfoProvider.MODEL_B, 512,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.EGOMAN, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "000e", RaspberryPiBoardInfoProvider.MODEL_B, 512,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.SONY, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "000f", RaspberryPiBoardInfoProvider.MODEL_B, 512,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.QISDA, "2.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0010", RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.SONY, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0011", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_1,
				512, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0012", RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.SONY, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0013", RaspberryPiBoardInfoProvider.MODEL_B_PLUS, 512,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.EGOMAN, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0014", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_1,
				512, Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "0015", RaspberryPiBoardInfoProvider.MODEL_A_PLUS, 256,
				List.of(new PinInfo("GPIO", "J8", 27, 13, "GPIO27", PinInfo.DIGITAL_IN_OUT, 27, 0, 27),
						new PinInfo("GPIO", "J8", 2, 3, "SDA1", PinInfo.DIGITAL_IN_OUT, 2, 0, 2),
						new PinInfo("GPIO", "J8", 3, 5, "SCL1", PinInfo.DIGITAL_IN_OUT, 3, 0, 3)),
				RaspberryPiBoardInfoProvider.EMBEST, "1.1");

		// PiA+, BCM2835, Sony, 512
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900021", RaspberryPiBoardInfoProvider.MODEL_A_PLUS,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");

		// PiB+, BCM2835, Sony, 512
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900032", RaspberryPiBoardInfoProvider.MODEL_B_PLUS,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");

		// Pi2B
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02042", RaspberryPiBoardInfoProvider.MODEL_2B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");
		// Pi2B, BCM2836, Sony, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2836, "a01040", RaspberryPiBoardInfoProvider.MODEL_2B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");
		// leonard - Pi2B, BCM2836, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2836, "a01041", RaspberryPiBoardInfoProvider.MODEL_2B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		// Pi2B, BCM2836, Embest, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2836, "a21041", RaspberryPiBoardInfoProvider.MODEL_2B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.1");
		// Pi2B, BCM2837, Embest, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a22042", RaspberryPiBoardInfoProvider.MODEL_2B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.2");

		// Pi3A+
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "9020e0", RaspberryPiBoardInfoProvider.MODEL_3A_PLUS,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");

		// stuart - Pi3B, BCM2837, Sony, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02082", RaspberryPiBoardInfoProvider.MODEL_3B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02082", RaspberryPiBoardInfoProvider.MODEL_3B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");
		// Pi3B, BCM2837, Embest, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a22082", RaspberryPiBoardInfoProvider.MODEL_3B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.2");
		// Pi3B, BCM2837, Sony Japan, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a32082", RaspberryPiBoardInfoProvider.MODEL_3B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY_JAPAN, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a52082", RaspberryPiBoardInfoProvider.MODEL_3B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.STADIUM, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a22083", RaspberryPiBoardInfoProvider.MODEL_3B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.3");
		// Pi3B+
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a020d3", RaspberryPiBoardInfoProvider.MODEL_3B_PLUS,
				_1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.3");

		// PiCM3, BCM2837, Sony, 1024MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a020a2", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3,
				_1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900061", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_1,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a020a0", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3,
				_1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a220a0", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3,
				_1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.0");
		// CM3+
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "a02100",
				RaspberryPiBoardInfoProvider.COMPUTE_MODULE_3_PLUS, _1GB, Collections.emptyList(),
				RaspberryPiBoardInfoProvider.SONY, "1.0");

		// Pi4B
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "a03111", RaspberryPiBoardInfoProvider.MODEL_4B, _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "b03111", RaspberryPiBoardInfoProvider.MODEL_4B, 2 * _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03111", RaspberryPiBoardInfoProvider.MODEL_4B, 4 * _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "b03115", RaspberryPiBoardInfoProvider.MODEL_4B, 2 * _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.5");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03115", RaspberryPiBoardInfoProvider.MODEL_4B, 4 * _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.5");

		// Pi5B
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2712, "d04170", RaspberryPiBoardInfoProvider.MODEL_5B, 8 * _1GB,
				Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");

		// Zero
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "920092", RaspberryPiBoardInfoProvider.MODEL_ZERO,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.2");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "920093", RaspberryPiBoardInfoProvider.MODEL_ZERO,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.3");

		// matt, shirley - Pi Zero, BCM2835, Sony, 512MB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900092", RaspberryPiBoardInfoProvider.MODEL_ZERO,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.2");
		// Pi Zero with DSI camera connector?
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "900093", RaspberryPiBoardInfoProvider.MODEL_ZERO,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.3");
		// obi - Pi Zero W
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "9000c1", RaspberryPiBoardInfoProvider.MODEL_ZERO_W,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		// ??
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2835, "9200c1", RaspberryPiBoardInfoProvider.MODEL_ZERO_W,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.EMBEST, "1.1");

		// Zero2W
		// Pi Zero 2 W v1.0
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2837, "902120", RaspberryPiBoardInfoProvider.MODEL_ZERO_2_W,
				_1GB / 2, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");

		// Pi 400
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03130", RaspberryPiBoardInfoProvider.MODEL_400,
				4 * _1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03131", RaspberryPiBoardInfoProvider.MODEL_400,
				4 * _1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");

		// CM4
		// Compute Module 4 v1.0 eMMC 1GB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "a03140", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_4,
				_1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");
		// Compute Module 4 v1.0 Lite 2GB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "b03140", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_4,
				2 * _1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");
		// Compute Module 4 v1.1 WiFi 4GB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "c03141", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_4,
				4 * _1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.1");
		// Compute Module 4 v1.0 WiFi 8GB
		validatePiBoard(RaspberryPiBoardInfoProvider.BCM2711, "d03140", RaspberryPiBoardInfoProvider.COMPUTE_MODULE_4,
				8 * _1GB, Collections.emptyList(), RaspberryPiBoardInfoProvider.SONY, "1.0");
	}

	private static void validatePiBoard(String processor, String revision, String model, int memoryKb,
			Collection<PinInfo> expectedPins, String expectedManufacturer, String expectedPcbRevision) {
		PiBoardInfo pi_board_info = (PiBoardInfo) validateLinuxArmBoard(
				List.of(RaspberryPiBoardInfoProvider.MAKE + "," + model.toLowerCase(),
						RaspberryPiBoardInfoProvider.BROADCOM + "," + processor.toLowerCase()),
				model, revision, memoryKb, expectedPins);
		Assertions.assertEquals(processor, pi_board_info.getProcessor());
		Assertions.assertEquals(expectedManufacturer, pi_board_info.getManufacturer());
		Assertions.assertEquals(expectedPcbRevision, pi_board_info.getPcbRevision());
	}

	private static BoardInfo validateLinuxArmBoard(List<String> compatible, String model, String revision, int memoryKb,
			Collection<PinInfo> expectedPins) {
		LocalSystemInfo sys_info = new LocalSystemInfo("Linux", "aarch64", compatible.get(0).split(",")[0], model,
				compatible.get(compatible.size() - 1), revision, memoryKb, compatible);
		BoardInfo board_info = LocalBoardInfoUtil.resolveLocalBoardInfo(sys_info);
		Assertions.assertEquals(sys_info.getMake().toLowerCase(), board_info.getMake().toLowerCase());
		Assertions.assertEquals(model, board_info.getModel());
		if (memoryKb != -1) {
			Assertions.assertEquals(memoryKb, board_info.getMemoryKb());
		}

		board_info.populateBoardPinInfo();

		expectedPins.forEach(pin_info -> validatePin(pin_info, board_info.getByName(pin_info.getName())));

		return board_info;
	}

	private static void validatePin(PinInfo expectedPinInfo, PinInfo resolvedPinInfo) {
		Assertions.assertNotNull(resolvedPinInfo);
		Assertions.assertEquals(expectedPinInfo.getName(), resolvedPinInfo.getName());
		Assertions.assertEquals(expectedPinInfo.getHeader(), resolvedPinInfo.getHeader());
		Assertions.assertEquals(expectedPinInfo.getDeviceNumber(), resolvedPinInfo.getDeviceNumber());
		Assertions.assertEquals(expectedPinInfo.getPhysicalPin(), resolvedPinInfo.getPhysicalPin());
		Assertions.assertEquals(expectedPinInfo.getSysFsNumber(), resolvedPinInfo.getSysFsNumber());
		Assertions.assertEquals(expectedPinInfo.getPwmChip(), resolvedPinInfo.getPwmChip());
		Assertions.assertEquals(expectedPinInfo.getPwmNum(), resolvedPinInfo.getPwmNum());
		Assertions.assertEquals(expectedPinInfo.getChip(), resolvedPinInfo.getChip());
		Assertions.assertEquals(expectedPinInfo.getLineOffset(), resolvedPinInfo.getLineOffset());
		Assertions.assertEquals(expectedPinInfo.getModes(), resolvedPinInfo.getModes());
	}
}
