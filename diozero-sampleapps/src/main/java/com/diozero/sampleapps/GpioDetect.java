package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.internal.provider.sysfs.GpioLine;
import com.diozero.internal.provider.sysfs.NativeGpioChip;

public class GpioDetect {
	public static void main(String[] args) {
		if (args.length == 0) {
			NativeGpioChip.getChips().forEach(chip -> System.out.format("%s [%s] (%d lines)%n", chip.getName(),
					chip.getLabel(), Integer.valueOf(chip.getNumLines())));
			return;
		}

		String chip_name;
		try {
			// See if the argument is a number
			chip_name = "gpiochip" + Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			chip_name = args[0];
		}

		try (NativeGpioChip chip = NativeGpioChip.openChip("/dev/" + chip_name)) {
			if (chip == null) {
				Logger.error("Unable to open chip {}", chip_name);
				return;
			}

			System.out.format("Opened Chip: %s [%s] - (%d lines)%n", chip.getName(), chip.getLabel(),
					Integer.valueOf(chip.getNumLines()));

			for (GpioLine line : chip.getLines()) {
				String consumer = line.getConsumer();
				if (consumer == null) {
					consumer = "unused";
				} else {
					consumer = "\"" + consumer + "\"";
				}
				System.out.format("line %3d: %18s %15s %6s %11s%s%n", Integer.valueOf(line.getOffset()),
						"\"" + line.getName() + "\"", consumer,
						line.getDirection() == GpioLine.Direction.INPUT ? "input" : "output",
						line.isActiveLow() ? "active-low" : "active-high", line.isReserved() ? " [used]" : "");
			}
		}
	}
}
