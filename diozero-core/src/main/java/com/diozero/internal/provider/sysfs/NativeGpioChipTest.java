package com.diozero.internal.provider.sysfs;

import java.util.List;

import org.tinylog.Logger;

import com.diozero.util.SleepUtil;

public class NativeGpioChipTest {
	private static final int ITERATIONS = 1_000_000;

	public static void main(String[] args) {
		List<GpioChipInfo> chips = NativeGpioChip.getChips();
		if (chips == null) {
			Logger.error("Failed to get chips");
			return;
		}
		chips.forEach(chip -> Logger.info("Chip {} ({}) - {} lines", chip.getName(), chip.getLabel(),
				Integer.valueOf(chip.getNumLines())));

		GpioChipInfo chip_info = chips.get(0);
		NativeGpioChip chip = NativeGpioChip.openChip("/dev/" + chip_info.getName());
		if (chip == null) {
			Logger.error("Unable to open chip {}", chip);
			return;
		}
		Logger.info("Got Chip {} ({}) - {} lines", chip.getName(), chip.getLabel(),
				Integer.valueOf(chip.getNumLines()));
		for (GpioLine line : chip.getLines()) {
			String consumer = line.getConsumer();
			if (consumer == null) {
				consumer = "unused";
			} else {
				consumer = "\"" + consumer + "\"";
			}
			Logger.info("line\t{}:\t\"{}\"\t{}\t{}\t{}{}", Integer.valueOf(line.getOffset()), line.getName(), consumer,
					line.getDirection() == GpioLine.Direction.INPUT ? "input" : "output",
					line.isActiveLow() ? "active-low" : "active-high", line.isReserved() ? " [used]" : "");
		}

		int gpio_num = 18;
		GpioLine line = chip.getLineByGpioNumber(gpio_num);
		if (line == null) {
			Logger.error("No line for GPIO number {}", Integer.valueOf(gpio_num));
			return;
		}
		int gpio_offset = line.getOffset();

		Logger.info("Setting GPIO {} to output...", Integer.valueOf(gpio_num));
		chip.provisionGpioOutputDevice(gpio_offset, 0);

		Logger.info("Setting GPIO {} value to 1...", Integer.valueOf(gpio_num));
		chip.setValue(gpio_offset, 1);
		Logger.info("GPIO {} value: {}", Integer.valueOf(gpio_num),
				Integer.valueOf(chip.getValue(gpio_offset)));
		SleepUtil.sleepSeconds(1);

		Logger.info("Setting GPIO {} value to 0...", Integer.valueOf(gpio_num));
		chip.setValue(gpio_offset, 0);
		Logger.info("GPIO {} value: {}", Integer.valueOf(gpio_num),
				Integer.valueOf(chip.getValue(gpio_offset)));
		SleepUtil.sleepSeconds(1);

		Logger.info("Toggling GPIO {} {} times", Integer.valueOf(gpio_num), Integer.valueOf(ITERATIONS));
		long start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			chip.setValue(gpio_offset, 1);
			chip.setValue(gpio_offset, 0);
		}
		long duration = System.currentTimeMillis() - start;
		Logger.info("Took {}ms for {} toggle iterations", Long.valueOf(duration), Integer.valueOf(ITERATIONS));
	}
}
