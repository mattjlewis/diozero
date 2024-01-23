package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     Rf433Sender.java
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

import java.util.concurrent.locks.LockSupport;

import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.DeviceFactoryHelper;

public class Rf433Sender {
	public static void main(String[] args) {
		int gpio = 26;
		int iterations = 10;
		int delay_ns = 300_000;
		try (MmapGpioInterface mmap_gpio = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo()
				.createMmapGpio()) {
			for (int i = 0; i < iterations; i++) {
				mmap_gpio.gpioWrite(gpio, false);
				LockSupport.parkNanos(delay_ns);
				mmap_gpio.gpioWrite(gpio, true);
				LockSupport.parkNanos(delay_ns);
			}
		}
	}
}
