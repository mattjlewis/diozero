package com.diozero.internal.board.chip;

import java.util.*;

import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class CHIPBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "CHIP";
	
	private static final CHIPBoardInfo CHIP_BOARD_INFO = new CHIPBoardInfo();

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.startsWith("Allwinner sun4i/sun5i")) {
			return CHIP_BOARD_INFO;
		}
		return null;
	}

	public static final class CHIPBoardInfo extends BoardInfo {
		private static Map<Integer, List<Mode>> CHIP_PINS;
		static {
			List<Mode> digital_in_out = Arrays.asList(
					GpioDeviceInterface.Mode.DIGITAL_INPUT,
					GpioDeviceInterface.Mode.DIGITAL_OUTPUT,
					GpioDeviceInterface.Mode.SOFTWARE_PWM_OUTPUT);

			CHIP_PINS = new HashMap<>();
			for (int i=0; i<8; i++) {
				CHIP_PINS.put(Integer.valueOf(i), digital_in_out);
			}
		}
		
		public CHIPBoardInfo() {
			super(MAKE, "CHIP", 1024, CHIP_PINS, MAKE.toLowerCase());
		}
	}
}
