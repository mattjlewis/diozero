package com.diozero.internal.board.radxa;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     Rock4BoardInfoProvider.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import com.diozero.internal.board.soc.rockchip.RockchipRK3399MmapGpio;
import com.diozero.internal.spi.BoardInfoProvider;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;

public class Rock4BoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "Radxa";
	private static final String ROCK4CPLUS_BOARD_HARDWARE_ID = "Radxa ROCK 4C+";
	private static final String ROCKPI4CPLUS_BOARD_HARDWARE_ID = "Radxa ROCK Pi 4C+";

	@Override
	public BoardInfo lookup(LocalSystemInfo localSysInfo) {
		if (localSysInfo.getHardware() != null && (localSysInfo.getHardware().equals(ROCK4CPLUS_BOARD_HARDWARE_ID)
				|| localSysInfo.getHardware().equals(ROCKPI4CPLUS_BOARD_HARDWARE_ID))) {
			return new Rock4BoardInfo(localSysInfo);
		}
		return null;
	}

	public static class Rock4BoardInfo extends GenericLinuxArmBoardInfo {
		Rock4BoardInfo(LocalSystemInfo localSysInfo) {
			super(localSysInfo, MAKE);
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new RockchipRK3399MmapGpio();
		}
	}
}
