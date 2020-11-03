package com.diozero.sampleapps;

import java.util.Collections;
import java.util.Map;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.MmapGpioInterface;
import com.diozero.util.BoardInfo;
import com.diozero.util.DeviceFactoryHelper;

public class GpioReadAll {
	public static void main(String[] args) {
		BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();
		MmapGpioInterface mmap_gpio = board_info.createMmapGpio();
		if (mmap_gpio == null) {
			Logger.error("Unable to load MMAP GPIO interface");
			return;
		}
		mmap_gpio.initialise();

		Map<Integer, PinInfo> pins = board_info.getHeaders().get(PinInfo.DEFAULT_HEADER);
		if (pins == null) {
			Logger.error("Unable to resolve pins for header {}", PinInfo.DEFAULT_HEADER);
			return;
		}

		// Get the maximum pin name length
		int max_name_length = Math.max(8,
				pins.values().stream().mapToInt(pin_info -> pin_info.getName().length()).max().orElse(8));

		String name_dash = String.join("", Collections.nCopies(max_name_length, "-"));
		System.out.format("Pins for header %s:%n", PinInfo.DEFAULT_HEADER);
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
		System.out.format("+ GP# + %" + max_name_length + "s + Mode + V +  gpiod + Physical + gpiod  + V + Mode + %-"
				+ max_name_length + "s + GP# +%n", "Name", "Name");
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);

		int index = 0;
		for (PinInfo pin_info : pins.values()) {
			int gpio = pin_info.getDeviceNumber();
			if (index++ % 2 == 0) {
				System.out.format("| %3s | %" + max_name_length + "s | %4s | %1s | %6s | %2s |",
						getNotDefined(gpio), pin_info.getName(), getModeString(mmap_gpio, gpio),
						gpio == PinInfo.NOT_DEFINED ? " " : mmap_gpio.gpioRead(gpio) ? "1" : "0",
						getGpiodName(pin_info.getChip(), pin_info.getLineOffset()),
						getNotDefined(pin_info.getPhysicalPin()));
			} else {
				System.out.format("| %-2s | %-6s | %1s | %-4s | %-" + max_name_length + "s | %-3s |%n",
						getNotDefined(pin_info.getPhysicalPin()), getGpiodName(pin_info.getChip(), pin_info.getLineOffset()),
						gpio == PinInfo.NOT_DEFINED ? " " : mmap_gpio.gpioRead(gpio) ? "1" : "0",
						getModeString(mmap_gpio, gpio), pin_info.getName(), getNotDefined(gpio));
			}
		}
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
	}

	private static String getGpiodName(int chip, int lineOffset) {
		if (chip == PinInfo.NOT_DEFINED || lineOffset == PinInfo.NOT_DEFINED) {
			return "";
		}
		return String.format("%2s:%-3s", Integer.valueOf(chip), Integer.valueOf(lineOffset));
	}

	private static String getModeString(MmapGpioInterface mmapGpio, int gpio) {
		if (gpio == PinInfo.NOT_DEFINED) {
			return "";
		}

		switch (mmapGpio.getMode(gpio)) {
		case DIGITAL_OUTPUT:
			return "Out";
		case DIGITAL_INPUT:
			return "In";
		default:
			return "Unkn";
		}
	}

	public static String getNotDefined(int value) {
		return value == PinInfo.NOT_DEFINED ? "" : Integer.toString(value);
	}
}
