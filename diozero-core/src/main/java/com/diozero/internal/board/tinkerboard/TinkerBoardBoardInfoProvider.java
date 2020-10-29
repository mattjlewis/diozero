package com.diozero.internal.board.tinkerboard;

import com.diozero.internal.board.GenericLinuxArmBoardInfo;
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

	public static class TinkerBoardBoardInfo extends GenericLinuxArmBoardInfo {
		public static final String MODEL = "Tinker Board";
		private static final int MEMORY_KB = 2_048_000;
		// private static final String LIBRARY_PATH = "tinkerboard";
		private static final String LIBRARY_PATH = "linux-arm";

		private TinkerBoardBoardInfo() {
			super(MAKE, MODEL, Integer.valueOf(MEMORY_KB), LIBRARY_PATH);
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
