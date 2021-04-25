package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     GpioDetect.java
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

import org.tinylog.Logger;

import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.internal.provider.builtin.gpio.GpioLine;

public class GpioDetect {
	public static void main(String[] args) {
		if (args.length == 0) {
			GpioChip.getChips().forEach(chip -> System.out.format("%s [%s] (%d lines)%n", chip.getName(),
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

		try (GpioChip chip = GpioChip.openChip("/dev/" + chip_name)) {
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
