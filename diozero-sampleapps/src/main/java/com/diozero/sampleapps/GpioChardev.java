package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     GpioChardev.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2026 diozero
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

import java.io.IOException;
import java.util.Collection;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.sbc.DeviceFactoryHelper;

public class GpioChardev {
	public static void main(String[] args) throws IOException {
		final Collection<GpioChip> chips = GpioChip.openAllChips().values();
		try {
			final BoardPinInfo bpi = DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo();
			chips.forEach(chip -> testLines(bpi, chip));
		} finally {
			chips.forEach(GpioChip::close);
		}
	}

	private static void testLines(BoardPinInfo bpi, GpioChip gpioChip) {
		for (GpioLine line : gpioChip.getLines()) {
			try {
				final PinInfo pin_info = bpi.getByChipAndLineOffsetOrThrow(gpioChip.getChipId(), line.getOffset());
				final DigitalInputDevice did = DigitalInputDevice.Builder.builder(pin_info).build();
				Logger.info("Provisioned digital input device gpiochip #{} line #{}, value: {}",
						Integer.valueOf(gpioChip.getChipId()), Integer.valueOf(line.getOffset()),
						Boolean.valueOf(did.getValue()));
			} catch (Throwable t) {
				Logger.info("Unable to provision digital input device gpiochip #{} line #{}: {}",
						Integer.valueOf(gpioChip.getChipId()), Integer.valueOf(line.getOffset()), t);
			}
		}
	}
}
