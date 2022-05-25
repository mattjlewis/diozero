package com.diozero.internal.board;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MmapGpioTester.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

public class MmapGpioTester {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <gpio>", MmapGpioTester.class.getName());
			return;
		}

		int gpio = Integer.parseInt(args[0]);

		try (NativeDeviceFactoryInterface ndf = DeviceFactoryHelper.getNativeDeviceFactory();
				MmapGpioInterface mmap_gpio = ndf.getBoardInfo().createMmapGpio()) {
			if (mmap_gpio == null) {
				Logger.error("Memory mapped GPIO not available");
				return;
			}
			mmap_gpio.initialise();

			DeviceMode start_mode = mmap_gpio.getMode(gpio);
			boolean start_value = mmap_gpio.gpioRead(gpio);
			Logger.info("Initial mode for GPIO {}: {}, value: ", Integer.valueOf(gpio), start_mode,
					Boolean.valueOf(start_value));

			DeviceMode new_mode = DeviceMode.DIGITAL_OUTPUT;
			mmap_gpio.setMode(gpio, new_mode);
			DeviceMode mode = mmap_gpio.getMode(gpio);
			if (mode != new_mode) {
				Logger.error("Failed to set GPIO {} mode to {}, detected mode: {}", Integer.valueOf(gpio), new_mode,
						mode);
				return;
			}

			boolean new_value = true;
			mmap_gpio.gpioWrite(gpio, new_value);
			boolean value = mmap_gpio.gpioRead(gpio);
			if (value != new_value) {
				Logger.error("Failed to set GPIO {} value to {}, detected value: {}", Integer.valueOf(gpio),
						Boolean.valueOf(new_value), Boolean.valueOf(value));
				return;
			}

			new_value = false;
			mmap_gpio.gpioWrite(gpio, new_value);
			value = mmap_gpio.gpioRead(gpio);
			if (value != new_value) {
				Logger.error("Failed to set GPIO {} value to {}, detected value: {}", Integer.valueOf(gpio),
						Boolean.valueOf(new_value), Boolean.valueOf(value));
				return;
			}

			new_mode = DeviceMode.DIGITAL_INPUT;
			mmap_gpio.setMode(gpio, new_mode);
			mode = mmap_gpio.getMode(gpio);
			if (mode != new_mode) {
				Logger.error("Failed to set GPIO {} mode to {}, detected mode: {}", Integer.valueOf(gpio), new_mode,
						mode);
				return;
			}

			GpioPullUpDown new_pud = GpioPullUpDown.PULL_DOWN;
			mmap_gpio.setPullUpDown(gpio, new_pud);
			value = mmap_gpio.gpioRead(gpio);
			// Should be false assuming nothing is connected
			if (value) {
				Logger.error("Failed to set GPIO {} pud to {}", Integer.valueOf(gpio), new_pud);
				return;
			}

			new_pud = GpioPullUpDown.PULL_UP;
			mmap_gpio.setPullUpDown(gpio, new_pud);
			value = mmap_gpio.gpioRead(gpio);
			// Should be true assuming nothing is connected
			if (!value) {
				Logger.error("Failed to set GPIO {} pud to {}", Integer.valueOf(gpio), new_pud);
				return;
			}

			Logger.info("Done - all tests past.");

			mmap_gpio.setMode(gpio, start_mode);
			if (start_mode == DeviceMode.DIGITAL_OUTPUT) {
				mmap_gpio.gpioWrite(gpio, start_value);
			}
		}
	}
}
