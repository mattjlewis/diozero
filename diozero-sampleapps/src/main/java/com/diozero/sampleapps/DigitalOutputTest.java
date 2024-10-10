package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     DigitalOutputTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PinInfo;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

public class DigitalOutputTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} [<gpio-num>|<gpiochip> <offset>]", DigitalOutputTest.class.getSimpleName());
			return;
		}

		try (GpioDeviceFactoryInterface df = DeviceFactoryHelper.getNativeDeviceFactory()) {
			final BoardPinInfo bpi = df.getBoardPinInfo();
			PinInfo pin;
			if (args.length == 1) {
				final int gpio_num = Integer.parseInt(args[0]);
				pin = bpi.getByGpioNumberOrThrow(gpio_num);
				Logger.info("Resolved pin for GPIO #{}: {}", Integer.valueOf(gpio_num), pin);
			} else {
				final int gpiochip = Integer.parseInt(args[0]);
				final int line = Integer.parseInt(args[1]);
				pin = bpi.getByChipAndLineOffsetOrThrow(gpiochip, line);
				Logger.info("Resolved pin for GPIO Chip {}, line {}: {}", Integer.valueOf(gpiochip),
						Integer.valueOf(line), pin);
			}
			try (DigitalOutputDevice dod = DigitalOutputDevice.Builder.builder(pin).build()) {
				System.out.println("Starting value: " + dod.isOn());
				for (int i = 0; i < 2 * 10; i++) {
					SleepUtil.sleepMillis(200);
					dod.toggle();
					System.out.println("Toggled, value now: " + dod.isOn());
				}
				dod.toggle();
				System.out.println("Toggled, value now: " + dod.isOn());
			}
		} catch (Exception e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
