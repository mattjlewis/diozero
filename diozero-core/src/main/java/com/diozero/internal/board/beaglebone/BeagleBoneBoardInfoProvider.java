package com.diozero.internal.board.beaglebone;

import java.util.*;

import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class BeagleBoneBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "BeagleBone";
	private static final String BBB_HARDWARE_ID = "Generic AM33XX";
	public static final BoardInfo BBB_BOARD_INFO = new BeagleBoneBlackBoardInfo();

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.startsWith(BBB_HARDWARE_ID)) {
			return BBB_BOARD_INFO;
		}
		return null;
	}
	
	public static class BeagleBoneBlackBoardInfo extends BoardInfo {
		private static final String BB_BLACK = "Black";
		private static final int MEMORY = 512;
		private static final String BBB_LIB_PATH = MAKE.toLowerCase() + "/" + BB_BLACK.toLowerCase();
		private static Map<Integer, List<Mode>> BBB_PINS;
		static {
			List<Mode> digital_in_out = Arrays.asList(
					GpioDeviceInterface.Mode.DIGITAL_INPUT,
					GpioDeviceInterface.Mode.DIGITAL_OUTPUT,
					GpioDeviceInterface.Mode.SOFTWARE_PWM_OUTPUT);

			BBB_PINS = new HashMap<>();
			BBB_PINS.put(Integer.valueOf(20), digital_in_out);
			BBB_PINS.put(Integer.valueOf(26), digital_in_out);
			BBB_PINS.put(Integer.valueOf(27), digital_in_out);
			BBB_PINS.put(Integer.valueOf(45), digital_in_out);
			BBB_PINS.put(Integer.valueOf(46), digital_in_out);
			BBB_PINS.put(Integer.valueOf(47), digital_in_out);
			BBB_PINS.put(Integer.valueOf(48), digital_in_out);
			BBB_PINS.put(Integer.valueOf(49), digital_in_out);
			BBB_PINS.put(Integer.valueOf(60), digital_in_out);
			BBB_PINS.put(Integer.valueOf(61), digital_in_out);
			BBB_PINS.put(Integer.valueOf(66), digital_in_out);
			BBB_PINS.put(Integer.valueOf(67), digital_in_out);
			BBB_PINS.put(Integer.valueOf(68), digital_in_out);
			BBB_PINS.put(Integer.valueOf(69), digital_in_out);
			BBB_PINS.put(Integer.valueOf(112), digital_in_out);
			BBB_PINS.put(Integer.valueOf(115), digital_in_out);
			BBB_PINS.put(Integer.valueOf(117), digital_in_out);
		}
		
		public BeagleBoneBlackBoardInfo() {
			super(MAKE, BB_BLACK, MEMORY, BBB_PINS, BBB_LIB_PATH);
		}
	}
}
