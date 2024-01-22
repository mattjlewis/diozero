package com.diozero.internal.soc.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerH3MmapGpio.java
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

import com.diozero.api.DeviceMode;
import com.diozero.util.SleepUtil;

/*-
 * https://wiki.friendlyarm.com/wiki/images/4/4b/Allwinner_H3_Datasheet_V1.2.pdf
 * See https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/wiringPi.c
 *
 *
 * Page 316 User Manual
 * CPU_PORT PIO base address 0x01C2_0800
 * 7 ports (PA, PC, PD, PE, PF, PG, PL)
 * 
 * Port Name  Bank  Num Pins
 * PA         0     22
 * PC         2     19
 * PD         3     18
 * PE         4     16
 * PF         5     7
 * PG         6     14
 * 
 * Register  Offset       Description
 * Pn_CFG0   n*0x24+0x00  Port n Configure Register 0 (n=0,2,3,4,5,6) Pn0..7_CFG (3+1 bits per GPIO)
 * Pn_CFG1   n*0x24+0x04  Port n Configure Register 1 (n=0,2,3,4,5,6) Pn8..15_CFG (3+1 bits per GPIO)
 * Pn_CFG2   n*0x24+0x08  Port n Configure Register 2 (n=0,2,3,4,5,6) Pn16..23_CFG (3+1 bits per GPIO)
 * Pn_CFG3   n*0x24+0x0c  Port n Configure Register 3 (n=0,2,3,4,5,6) Pn24..31_CFG (3+1 bits per GPIO)
 * Pn_DAT    n*0x24+0x10  Port n Data Register (n=0,2,3,4,5,6) Pn_DAT (1 bit per GPIO)
 * Pn_DRV0   n*0x24+0x14  Port n Multi-Driving Register 0 (n=0,2,3,4,5,6) Pn0..15_DRV (2 bits per GPIO)
 * Pn_DRV1   n*0x24+0x18  Port n Multi-Driving Register 1 (n=0,2,3,4,5,6) Pn16..31_DRV (2 bits per GPIO)
 * Pn_PUL0   n*0x24+0x1c  Port n Pull Register 0 (n=0,2,3,4,5,6) Pn0..15_PULL (2 bits per GPIO)
 * Pn_PUL1   n*0x24+0x20  Port n Pull Register 1 (n=0,2,3,4,5,6) Pn16..31_PULL
 * 
 * Page 433 User Manual
 * CPUS_PORT PIO base address 0x01F0_2C00
 * 1 port (PL)
 * 
 * Port Name  Bank  Num Pins
 * PL         11    12
 * 
 * Register offsets as per CPU_PORT
 */
public class AllwinnerH3MmapGpio extends AllwinnerHMmapGpio {
	private static final long GPIOA_BASE = 0x01C2_0000L;
	private static final int GPIOA_INT_OFFSET = 0x800 / 4;
	private static final long GPIOL_BASE = 0x01F0_2000L;
	private static final int GPIOL_INT_OFFSET = 0xC00 / 4;
	private static final int[] NUM_GPIOS_BY_BANK = { 22, 0, 19, 18, 16, 7, 14, 0, 0, 0, 0, 12 };

	public AllwinnerH3MmapGpio() {
		super(GPIOA_BASE, GPIOA_INT_OFFSET, GPIOL_BASE, GPIOL_INT_OFFSET, NUM_GPIOS_BY_BANK);
	}

	@Override
	void initialiseGpioModes() {
		// PA0 (0)
		addGpioMode(0, 2, DeviceMode.SERIAL);
		// PA1 (1)
		addGpioMode(1, 2, DeviceMode.SERIAL);
		// PA2 (2)
		addGpioMode(2, 2, DeviceMode.SERIAL);
		// PA3 (3)
		addGpioMode(3, 2, DeviceMode.SERIAL);
		// PA4 (4)
		addGpioMode(4, 2, DeviceMode.SERIAL);
		// PA5 (5)
		addGpioMode(5, 2, DeviceMode.SERIAL);
		addGpioMode(5, 3, DeviceMode.PWM_OUTPUT);
		// PA6 (6)
		addGpioMode(6, 3, DeviceMode.PWM_OUTPUT);
		// PA11 (11)
		addGpioMode(11, 2, DeviceMode.I2C);
		// PA12 (12)
		addGpioMode(12, 2, DeviceMode.I2C);
		// PA13 (13)
		addGpioMode(13, 2, DeviceMode.SPI);
		addGpioMode(13, 3, DeviceMode.SERIAL);
		// PA14 (14)
		addGpioMode(14, 2, DeviceMode.SPI);
		addGpioMode(14, 3, DeviceMode.SERIAL);
		// PA15 (15)
		addGpioMode(15, 2, DeviceMode.SPI);
		addGpioMode(15, 3, DeviceMode.SERIAL);
		// PA16 (16)
		addGpioMode(16, 2, DeviceMode.SPI);
		addGpioMode(16, 3, DeviceMode.SERIAL);
		// PA18 (18)
		addGpioMode(18, 3, DeviceMode.I2C);
		// PA19 (19)
		addGpioMode(19, 3, DeviceMode.I2C);

		// PC0 (32)
		addGpioMode(32, 3, DeviceMode.SPI);
		// PC1 (33)
		addGpioMode(33, 3, DeviceMode.SPI);
		// PC2 (34)
		addGpioMode(34, 3, DeviceMode.SPI);
		// PC3 (35)
		addGpioMode(35, 3, DeviceMode.SPI);

		// PF4 (164)
		addGpioMode(164, 3, DeviceMode.SERIAL);

		// PG6 (198)
		addGpioMode(198, 2, DeviceMode.SERIAL);
		// PG7 (199)
		addGpioMode(199, 2, DeviceMode.SERIAL);
		// PG8 (200)
		addGpioMode(200, 2, DeviceMode.SERIAL);
		// PG9 (201)
		addGpioMode(201, 2, DeviceMode.SERIAL);
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		// Computed values for GPIO mode register
		for (int gpio = 0; gpio < 224; gpio++) {
			int bank = gpio >> 5;
			int index = gpio - (bank << 5);
			int offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
			int shift = ((index - ((index >> 3) << 3)) << 2);

			int my_bank = gpio >> 5;
			int my_index = gpio % 32;
			int my_offset = (gpio >> 3) + 5 * (gpio >> 5);
			int my_shift = (gpio % 8) << 2;

			if (bank != my_bank || index != my_index || offset != my_offset || shift != my_shift) {
				System.out.format("Xx Mode for GPIO %d - bank: %d, index:%d, offset: 0x%02x, shift: %d%n", gpio, bank,
						index, offset, shift);
				System.out.format("My Mode for GPIO %d - bank: %d, index:%d, offset: 0x%02x, shift: %d%n", gpio,
						my_bank, my_index, my_offset, my_shift);
			}
			System.out.format("My Mode for GPIO %d - bank: %d, index:%d, offset: 0x%02x, shift: %d%n", gpio, my_bank,
					my_index, my_offset, my_shift);
		}

		// Computed values for GPIO value register
		for (int gpio = 0; gpio < 224; gpio++) {
			int bank = gpio >> 5;
			int gpio_int_offset = ((bank * 36) + 0x10) / 4;
			int shift = gpio - (bank << 5);

			int my_bank = gpio / 32;
			int my_offset = (gpio / 32) * 9 + 4;
			int my_shift = gpio % 32;

			if (bank != my_bank || gpio_int_offset != my_offset || shift != my_shift) {
				System.out.format("Xx Value for GPIO %d - bank: %d, offset: 0x%02x, shift: %d%n", gpio, bank,
						gpio_int_offset, shift);
				System.out.format("My Value for GPIO %d - bank: %d, offset: 0x%02x, shift: %d%n", gpio, my_bank,
						my_offset, my_shift);
			}
		}

		// Computed values for GPIO pull up/down control
		for (int gpio = 0; gpio < 224; gpio++) {
			int bank = gpio >> 5;
			int index = gpio - (bank << 5);
			int sub = index >> 4;
			int sub_index = index - 16 * sub;
			int int_offset = ((bank * 36) + 0x1c + sub * 4) / 4;

			int my_bank = gpio >> 5;
			int my_index = gpio % 32;
			int my_sub = (gpio >> 4) % 2;
			int my_sub_index = gpio % 16;
			int my_int_offset = (0x1c + my_bank * 0x24 + my_sub * 4) / 4;

			if (bank != my_bank || index != my_index || sub != my_sub || sub_index != my_sub_index
					|| int_offset != my_int_offset) {
				System.out.format(
						"Xx PUD for GPIO %d - bank: %d, index: %d, sub: %d, sub_index: %d, int_offset=0x%02x%n", gpio,
						bank, index, sub, sub_index, int_offset);
				System.out.format(
						"My PUD for GPIO %d - bank: %d, index: %d, sub: %d, sub_index: %d, int_offset=0x%02x%n", gpio,
						my_bank, my_index, my_sub, my_sub_index, my_int_offset);
			}
		}

		for (int gpio = 0; gpio < 224; gpio++) {
			int bank = gpio >> 5; // equivalent to / 32
			int reg = (gpio >> 4) % 2;
			int shift = (gpio % 16) << 1;
			int offset = 0x1c + bank * 0x24 + reg * 4;
			int int_offset = offset / 4;
			System.out.format("My PUD for GPIO %d - bank: %d, reg: %d, shift: %d, offset: 0x%02x, int_offset: 0x%02x%n",
					gpio, bank, reg, shift, offset, int_offset);
		}

		if (args.length < 1) {
			System.out.println("Usage: " + AllwinnerH3MmapGpio.class.getName() + " <gpio>");
			return;
		}
		int gpio = Integer.parseInt(args[0]);

		try (AllwinnerH3MmapGpio mmap_gpio = new AllwinnerH3MmapGpio()) {
			mmap_gpio.initialise();

			System.out.println("Mode: " + mmap_gpio.getMode(gpio));

			for (int i = 0; i < 20; i++) {
				System.out.println("Mode for GPIO #" + gpio + ": " + mmap_gpio.getMode(gpio));
				SleepUtil.sleepSeconds(1);
			}

			if (true) {
				return;
			}

			for (int i = 0; i < 5; i++) {
				System.out.println("GPIO#" + gpio);
				mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
				System.out.println("Mode: " + mmap_gpio.getMode(gpio));
				SleepUtil.sleepSeconds(0.5);
				mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_INPUT);
				System.out.println("Mode: " + mmap_gpio.getMode(gpio));
				SleepUtil.sleepSeconds(0.5);
			}

			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			for (int i = 0; i < 5; i++) {
				System.out.println("Value: " + mmap_gpio.gpioRead(gpio));

				mmap_gpio.gpioWrite(gpio, true);
				System.out.println("Value: " + mmap_gpio.gpioRead(gpio));

				System.out.println("Sleeping");
				SleepUtil.sleepSeconds(0.5);

				mmap_gpio.gpioWrite(gpio, false);
				System.out.println("Value: " + mmap_gpio.gpioRead(gpio));

				System.out.println("Sleeping");
				SleepUtil.sleepSeconds(0.5);
			}

			int iterations = 1_000_000;
			if (args.length > 1) {
				iterations = Integer.parseInt(args[1]);
			}

			for (int i = 0; i < 3; i++) {
				long start_ms = System.currentTimeMillis();

				for (int j = 0; j < iterations; j++) {
					mmap_gpio.gpioWrite(gpio, true);
					mmap_gpio.gpioWrite(gpio, false);
				}

				long duration_ms = System.currentTimeMillis() - start_ms;
				double frequency = iterations / (duration_ms / 1000.0);

				System.out.format("Duration for %,d iterations: %,.3f s, frequency: %,.0f Hz%n",
						Integer.valueOf(iterations), Float.valueOf(((float) duration_ms) / 1000),
						Double.valueOf(frequency));
			}
		}
	}
}
