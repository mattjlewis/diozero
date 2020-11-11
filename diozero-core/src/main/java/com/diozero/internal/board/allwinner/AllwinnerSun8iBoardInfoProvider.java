package com.diozero.internal.board.allwinner;

import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.provider.MmapGpioInterface;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class AllwinnerSun8iBoardInfoProvider implements BoardInfoProvider {
	@Override
	public BoardInfo lookup(String hardware, String revision, Integer memoryKb) {
		if (hardware != null && hardware.startsWith("Allwinner sun8i")) {
			return new AllwinnerSun8iBoardInfo(memoryKb);
		}
		return null;
	}

	public static class AllwinnerSun8iBoardInfo extends GenericLinuxArmBoardInfo {
		public AllwinnerSun8iBoardInfo(Integer memoryKb) {
			super("Allwinner", "sun8i", memoryKb);
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new AllwinnerSun8iMmapGpio();
		}
	}
}
