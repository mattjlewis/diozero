package com.diozero.internal.provider.builtin.gpio;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     GpioChipTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import java.util.List;

import org.tinylog.Logger;

import com.diozero.util.LibraryLoader;
import com.diozero.util.SleepUtil;

public class GpioChipTest {
	static {
		LibraryLoader.loadSystemUtils();
	}

	private static final int ITERATIONS = 1_000_000;

	public static void main(String[] args) {
		List<GpioChipInfo> chips = NativeGpioDevice.getChips();
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
		GpioChip gpio_chip = GpioChip.openChip("/dev/gpiochip" + chip_num);
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
		line.setValue(1);
		Logger.info("GPIO {} value: {}", Integer.valueOf(line_num), Integer.valueOf(line.getValue()));
		SleepUtil.sleepSeconds(1);

		Logger.info("Setting Line {} value to 0...", Integer.valueOf(line_num));
		line.setValue(0);
		Logger.info("GPIO {} value: {}", Integer.valueOf(line_num), Integer.valueOf(line.getValue()));
		SleepUtil.sleepSeconds(1);

		Logger.info("Toggling Line {} {} times", Integer.valueOf(line_num), Integer.valueOf(ITERATIONS));
		long start_ms = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			line.setValue(1);
			line.setValue(0);
		}
		long duration_ms = System.currentTimeMillis() - start_ms;
		double frequency = ITERATIONS / (duration_ms / 1000.0);
		Logger.info("Duration for {#,###} iterations: {#,###.#} ms, frequency {#,###} Hz", Long.valueOf(duration_ms),
				Integer.valueOf(ITERATIONS), Double.valueOf(frequency));
	}
}
