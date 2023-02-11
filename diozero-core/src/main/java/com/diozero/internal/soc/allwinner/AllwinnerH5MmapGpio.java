package com.diozero.internal.soc.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerH5MmapGpio.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

/*-
 * Links:
 * https://linux-sunxi.org/GPIO
 * https://linux-sunxi.org/Xunlong_Orange_Pi_Zero_Plus
 * https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/boardtype_friendlyelec.c#L66
 * https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/wiringPi.c#L536
 * https://github.com/orangepi-xunlong/wiringOP/blob/master/wiringPi/OrangePi.c#L622
 * https://linux-sunxi.org/images/a/a3/Allwinner_H5_Manual_v1.0.pdf
 * https://linux-sunxi.org/images/d/de/Allwinner_H5_Datasheet_V1.0.pdf
 * 
 * Page 291 User Manual
 * CPUX_PORT PIO base address 0x01C2_0800
 * 7 ports (PA, PC, PD, PE, PF, PG, PL)
 * 
 * Port Name  Bank  Num Pins
 * PA         0     22
 * PC         2     17
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
 * 2 ports (PL, PM)
 * 
 * Port Name  Bank  Num Pins
 * PL         11    12
 * 
 * Register offsets as per CPUX_PORT
 */
public class AllwinnerH5MmapGpio extends AllwinnerHMmapGpio {
	private static final long GPIOA_BASE = 0x01C2_0000L;
	private static final int GPIOA_INT_OFFSET = 0x0800 / 4;
	private static final long GPIOL_BASE = 0x01F0_2000L;
	private static final int GPIOL_INT_OFFSET = 0x0C00 / 4;
	private static final int[] NUM_GPIOS_BY_BANK = { 22, 0, 17, 18, 16, 7, 14, 0, 0, 0, 0, 12 };

	public AllwinnerH5MmapGpio() {
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

		// PC0 (64)
		addGpioMode(64, 3, DeviceMode.SPI);
		// PC1 (65)
		addGpioMode(65, 3, DeviceMode.SPI);
		// PC2 (66)
		addGpioMode(66, 3, DeviceMode.SPI);
		// PC3 (67)
		addGpioMode(67, 3, DeviceMode.SPI);
		// PC4 (68)
		addGpioMode(68, 4, DeviceMode.SPI);

		// PE12 (140)
		addGpioMode(140, 2, DeviceMode.I2C);
		// XXX Clash
		addGpioMode(140, 3, DeviceMode.I2C);
		// PE13 (173)
		addGpioMode(141, 2, DeviceMode.I2C);
		// XXX Clash
		addGpioMode(141, 3, DeviceMode.I2C);

		// PF2 (162)
		addGpioMode(162, 3, DeviceMode.SERIAL);
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

		// PL0 (352)
		addGpioMode(352, 2, DeviceMode.I2C);
		// PL1 (353)
		addGpioMode(353, 2, DeviceMode.I2C);
		// PL2 (354)
		addGpioMode(354, 2, DeviceMode.SERIAL);
		// PL3 (355)
		addGpioMode(355, 2, DeviceMode.SERIAL);
		// PL10 (362)
		addGpioMode(362, 2, DeviceMode.PWM_OUTPUT);
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		System.out.println((256 / 8) + ", " + (256 >> 3));
		System.out.println((256 / 16) + ", " + (256 >> 4));
		System.out.println((256 / 32) + ", " + (256 >> 5));
		System.out.println((352 / 32) + ", " + (352 >> 5));

		// Test bank 11 (GPIO >= 352) - not sure how to handle this
		int a_int_offset = 0x10 / 4 + 0xc00 / 4;
		int b_int_offset = (352 >> 5) * 9 + 4 + 0x800 / 4;
		System.out.println(a_int_offset + ", " + b_int_offset);

		// Test read / write offset and shift calculation
		for (int gpio = 0; gpio < 352; gpio++) {
			a_int_offset = (gpio >> 5) * 9 + 4;
			int a_shift = gpio % 32;

			int bank = gpio >> 5;
			b_int_offset = ((bank * 36) + 0x10) / 4;
			int b_shift = gpio % 32;

			if (a_int_offset != b_int_offset) {
				System.out.format("** Value Offset Difference - gpio: %d, a_int_offset: 0x%x, b_int_offset: 0x%x%n",
						gpio, a_int_offset, b_int_offset);
			}

			if (a_shift != b_shift) {
				System.out.format("** Value Shift Difference - gpio: %d, a_shift: %d, b_shift: %d%n", gpio, a_shift,
						b_shift);
			}
		}

		// Test mode offset and shift calculation
		for (int gpio = 0; gpio < 352; gpio++) {
			a_int_offset = (gpio >> 3) + 5 * (gpio >> 5);
			int a_shift = (gpio % 8) * 4;

			int bank = gpio >> 5;
			int index = gpio - (bank << 5);
			b_int_offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
			int b_shift = ((index - ((index >> 3) << 3)) << 2);

			if (a_int_offset != b_int_offset) {
				System.out.format("** Mode Offset Difference - gpio: %d, a_int_offset: 0x%x, b_int_offset: 0x%x%n",
						gpio, a_int_offset, b_int_offset);
			}

			if (a_shift != b_shift) {
				System.out.format("** Mode Shift Difference - gpio: %d, a_shift: %d, b_shift: %d%n", gpio, a_shift,
						b_shift);
			}
		}

		if (args.length < 1) {
			System.out.println("Usage: <gpio>");
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);

		try (AllwinnerH5MmapGpio mmap_gpio = new AllwinnerH5MmapGpio()) {
			mmap_gpio.initialise();

			DeviceMode mode = mmap_gpio.getMode(gpio);
			System.out.format("Mode for gpio %d: %s%n", Integer.valueOf(gpio), mode);

			DeviceMode new_mode = DeviceMode.DIGITAL_INPUT;
			mmap_gpio.setMode(gpio, new_mode);
			mode = mmap_gpio.getMode(gpio);
			System.out.format("Mode for gpio %d: %s (%s)%n", gpio, mode, mode == new_mode ? "Correct" : "Incorrect!");
			if (mode != new_mode) {
				return;
			}

			new_mode = DeviceMode.DIGITAL_OUTPUT;
			mmap_gpio.setMode(gpio, new_mode);
			mode = mmap_gpio.getMode(gpio);
			System.out.format("Mode for gpio %d: %s (%s)%n", gpio, mode, mode == new_mode ? "Correct" : "Incorrect!");
			if (mode != new_mode) {
				return;
			}

			/*-
			for (int i = 0; i < 10; i++) {
				System.out.println(mmap_gpio.gpioRead(gpio));
				SleepUtil.sleepSeconds(1);
			}
			 */

			for (int i = 0; i < 5; i++) {
				boolean new_value = false;
				mmap_gpio.gpioWrite(gpio, new_value);
				boolean value = mmap_gpio.gpioRead(gpio);
				System.out.format("Value for gpio %d: %b (%s)%n", gpio, value,
						value == new_value ? "Correct" : "Incorrect!");
				if (value != new_value) {
					return;
				}

				new_value = true;
				mmap_gpio.gpioWrite(gpio, new_value);
				value = mmap_gpio.gpioRead(gpio);
				System.out.format("Value for gpio %d: %b (%s)%n", gpio, value,
						value == new_value ? "Correct" : "Incorrect!");
				if (value != new_value) {
					return;
				}
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
