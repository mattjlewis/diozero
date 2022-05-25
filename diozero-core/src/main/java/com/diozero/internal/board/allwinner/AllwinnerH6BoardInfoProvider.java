package com.diozero.internal.board.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerH6BoardInfoProvider.java
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

/**
 * https://linux-sunxi.org/Xunlong_Orange_Pi_One_Plus
 * https://linux-sunxi.org/GPIO
 */
public class AllwinnerH6BoardInfoProvider implements BoardInfoProvider {
	// Not reliable
	// public static final String ORANGEPI_ONEPLUS_HARDWARE = "sun50iw1p1";

	// E.g. CONFIG_ORANGEPI_LITE2, CONFIG_ORANGEPI_3
	public static final String ORANGEPI_H6_HARDWARE = "sun50iw6";
	public static final String ORANGEPI_ONEPLUS_MODEL = "OrangePi One Plus";
	public static final String MAKE = "Allwinner H6";

	@Override
	public BoardInfo lookup(LocalSystemInfo sysInfo) {
		if ((sysInfo.getHardware() != null && sysInfo.getHardware().equals(ORANGEPI_H6_HARDWARE))
				|| (sysInfo.getModel() != null && sysInfo.getModel().equals(ORANGEPI_ONEPLUS_MODEL))) {
			return new AllwinnerH6BoardInfo(sysInfo);
		}
		return null;
	}

	public static class AllwinnerH6BoardInfo extends GenericLinuxArmBoardInfo {
		public AllwinnerH6BoardInfo(LocalSystemInfo localSysInfo) {
			super(localSysInfo, MAKE);
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new AllwinnerH6MmapGpio();
		}
	}
}
