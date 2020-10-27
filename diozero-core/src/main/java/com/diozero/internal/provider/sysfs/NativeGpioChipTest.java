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

		if (args.length == 0) {
			return;
		}

		int chip_num = Integer.parseInt(args[0]);
		NativeGpioChip gpio_chip = NativeGpioChip.openChip(chip_num);
		if (gpio_chip == null) {
			Logger.error("Unable to open chip {}", Integer.valueOf(chip_num));
			return;
		}

		Logger.info("Got Chip {} ({}) - {} lines", gpio_chip.getName(), gpio_chip.getLabel(),
				Integer.valueOf(gpio_chip.getNumLines()));
		for (GpioLine line : gpio_chip.getLines()) {
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

		if (args.length == 1) {
			return;
		}

		int line_num = Integer.parseInt(args[1]);
		GpioLine line = gpio_chip.getLines()[line_num];
		if (line == null) {
			Logger.error("No line for Line number {}", Integer.valueOf(line_num));
			return;
		}
		int gpio_offset = line.getOffset();

		Logger.info("Setting Line {} to output...", Integer.valueOf(line_num));
		gpio_chip.provisionGpioOutputDevice(gpio_offset, 0);

		Logger.info("Setting Line {} value to 1...", Integer.valueOf(line_num));
		gpio_chip.setValue(line, 1);
		Logger.info("GPIO {} value: {}", Integer.valueOf(line_num), Integer.valueOf(gpio_chip.getValue(line)));
		SleepUtil.sleepSeconds(1);

		Logger.info("Setting Line {} value to 0...", Integer.valueOf(line_num));
		gpio_chip.setValue(line, 0);
		Logger.info("GPIO {} value: {}", Integer.valueOf(line_num), Integer.valueOf(gpio_chip.getValue(line)));
		SleepUtil.sleepSeconds(1);

		Logger.info("Toggling Line {} {} times", Integer.valueOf(line_num), Integer.valueOf(ITERATIONS));
		long start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			gpio_chip.setValue(line, 1);
			gpio_chip.setValue(line, 0);
		}
		long duration = System.currentTimeMillis() - start;
		Logger.info("Took {}ms for {} toggle iterations", Long.valueOf(duration), Integer.valueOf(ITERATIONS));
	}
}
