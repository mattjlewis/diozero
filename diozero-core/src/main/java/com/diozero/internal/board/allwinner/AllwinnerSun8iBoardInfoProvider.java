package com.diozero.internal.board.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerSun8iBoardInfoProvider.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
public class AllwinnerSun8iBoardInfoProvider implements BoardInfoProvider {
	// E.g. CONFIG_ORANGEPI_H3, CONFIG_ORANGEPI_ZEROPLUS2_H3
	public static final String MAKE = "Allwinner sun8i";

	@Override
	public BoardInfo lookup(LocalSystemInfo sysInfo) {
		if (sysInfo.getHardware() != null && sysInfo.getHardware().startsWith(MAKE)) {
			return new AllwinnerSun8iBoardInfo(sysInfo);
		}
		return null;
	}

	public static class AllwinnerSun8iBoardInfo extends GenericLinuxArmBoardInfo {
		public AllwinnerSun8iBoardInfo(LocalSystemInfo localSysInfo) {
			super(localSysInfo, MAKE);
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new AllwinnerSun8iMmapGpio();
		}
	}
}
