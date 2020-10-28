package com.diozero.internal.board;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.util.BoardInfo;
import com.diozero.util.SystemInfoConstants;

public class UnknownBoardInfo extends BoardInfo {
	public static BoardInfo get(String model, String hardware, String revision, Integer memoryKb) {
		String os_name = System.getProperty(SystemInfoConstants.OS_NAME_SYSTEM_PROPERTY);
		String os_arch = System.getProperty(SystemInfoConstants.OS_ARCH_SYSTEM_PROPERTY);
		Logger.warn("Failed to resolve board info for hardware '{}' and revision '{}'. Local O/S: {}-{}", hardware,
				revision, os_name, os_arch);

		if (os_name.equals(SystemInfoConstants.LINUX_OS_NAME) && (os_arch.equals(SystemInfoConstants.ARM_32_OS_ARCH)
				|| os_arch.equals(SystemInfoConstants.ARM_64_OS_ARCH))) {
			return new GenericLinuxArmBoardInfo(model, revision, memoryKb, os_name, os_arch);
		}

		return new UnknownBoardInfo(model, memoryKb, os_name, os_arch);
	}

	public UnknownBoardInfo(String model, Integer memoryKb, String osName, String osArch) {
		super(UNKNOWN, model, memoryKb == null ? -1 : memoryKb.intValue(),
				osName.replace(" ", "").toLowerCase() + "-" + osArch.toLowerCase());
	}

	@Override
	public void initialisePins() {
	}

	@Override
	public PinInfo getByGpioNumber(int gpio) {
		PinInfo pin_info = super.getByGpioNumber(gpio);
		if (pin_info == null) {
			pin_info = addGpioPinInfo(gpio, gpio, PinInfo.DIGITAL_IN_OUT);
		}
		return pin_info;
	}

	@Override
	public PinInfo getByAdcNumber(int adcNumber) {
		PinInfo pin_info = super.getByAdcNumber(adcNumber);
		if (pin_info == null) {
			pin_info = addAdcPinInfo(adcNumber, adcNumber);
		}
		return pin_info;
	}

	@Override
	public PinInfo getByDacNumber(int dacNumber) {
		PinInfo pin_info = super.getByDacNumber(dacNumber);
		if (pin_info == null) {
			pin_info = addDacPinInfo(dacNumber, dacNumber);
		}
		return pin_info;
	}
}
