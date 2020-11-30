package com.diozero.sampleapps.perf;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     MmapGpioPerfTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.DeviceFactoryHelper;

public class MmapGpioPerfTest {
	private static final int ITERATIONS = 1_000_000;

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <pin-number> [<iterations>]", MmapGpioPerfTest.class.getName());
			System.exit(1);
		}

		int pin = Integer.parseInt(args[0]);

		int iterations = ITERATIONS;
		if (args.length > 1) {
			iterations = Integer.parseInt(args[1]);
		}

		test(pin, iterations);
	}

	public static void test(int gpio, int iterations) {
		try (MmapGpioInterface mmap_gpio = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo()
				.createMmapGpio()) {
			mmap_gpio.initialise();
			
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			for (int j = 0; j < 5; j++) {
				long start_nano = System.nanoTime();
				for (int i = 0; i < iterations; i++) {
					mmap_gpio.gpioWrite(gpio, true);
					mmap_gpio.gpioWrite(gpio, false);
				}
				long duration_ns = System.nanoTime() - start_nano;

				Logger.info("Duration for {} iterations: {}s", Integer.valueOf(iterations),
						String.format("%.4f", Float.valueOf(((float) duration_ns) / 1000 / 1000 / 1000)));
			}
		}
	}
}
