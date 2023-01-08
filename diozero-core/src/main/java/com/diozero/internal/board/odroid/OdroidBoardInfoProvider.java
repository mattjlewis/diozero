package com.diozero.internal.board.odroid;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OdroidBoardInfoProvider.java
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

import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.spi.BoardInfoProvider;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;

import aQute.bnd.annotation.spi.ServiceProvider;
@ServiceProvider(value = BoardInfoProvider.class)
public class OdroidBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "Odroid";
	public static final String C1_HARDWARE_ID = "ODROID-C1";
	public static final String C2_HARDWARE_ID = "ODROID-C2";
	public static final String N2_PLUS_HARDWARE_ID = "ODROID-N2Plus";

	public enum Model {
		C0, U2_U3, C1, XU_3_4, C2, N2_Plus
	}

	@Override
	public BoardInfo lookup(LocalSystemInfo localSysInfo) {
		String hardware = localSysInfo.getHardware();
		if (hardware != null) {
			if (hardware.equals(C1_HARDWARE_ID)) {
				return new OdroidC1BoardInfo();
			}
			if (hardware.equals(C2_HARDWARE_ID) || hardware.endsWith(C2_HARDWARE_ID)) {
				return new OdroidC2BoardInfo();
			}
			if (hardware.endsWith(N2_PLUS_HARDWARE_ID)) {
				return new OdroidN2PlusBoardInfo(localSysInfo.getMemoryKb().intValue());
			}
		}
		return null;
	}

	public static class OdroidC1BoardInfo extends GenericLinuxArmBoardInfo {
		private static final int MEMORY = 1_024_000;
		private static final float ADC_VREF = 1.8f;

		public OdroidC1BoardInfo() {
			super(MAKE, Model.C2.toString(), MEMORY, ADC_VREF);
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

		OdroidC2BoardInfo() {
			super(MAKE, Model.C2.toString(), MEMORY, ADC_VREF);
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new OdroidC2MmapGpio();
		}
	}

	public static class OdroidN2PlusBoardInfo extends GenericLinuxArmBoardInfo {
		private static final int MEMORY_2G = 2_048_000;
		private static final int MEMORY_4G = 4_096_000;
		private static final float ADC_VREF = 1.8F;

		OdroidN2PlusBoardInfo(int memory) {
			super(MAKE, Model.N2_Plus.toString(), memory > MEMORY_2G ? MEMORY_4G : MEMORY_2G, ADC_VREF);
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new OdroidN2PlusMmapGpio();
		}
	}
}
