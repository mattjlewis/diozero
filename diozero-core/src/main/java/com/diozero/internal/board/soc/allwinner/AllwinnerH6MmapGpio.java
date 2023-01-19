package com.diozero.internal.board.soc.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerH6MmapGpio.java
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
 * https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/wiringPi.c
 * https://github.com/orangepi-xunlong/wiringOP/blob/master/wiringPi/OrangePi.h#L70
 * https://linux-sunxi.org/images/4/46/Allwinner_H6_V200_User_Manual_V1.1.pdf
 * https://linux-sunxi.org/images/5/5c/Allwinner_H6_V200_Datasheet_V1.1.pdf
 * 
 * Page 379 User Manual
 * CPUX_PORT PIO base address 0x0300_b000
 * 5 ports (PC, PD, PF, PG, PH)
 * 
 * Port Name  Bank  Num Pins  Multiplex
 * PC         2     17        NAND/SDC/SPI
 * PD         3     27        LCD/TS/RGMII/CSI/DMIC/TWI/UART/JTAG/PWM
 * PF         5     7         SDC/UART/JTAG
 * PG         6     15        SDC/UART/PCM/SCR
 * PH         7     11        UART/PCM/OWA/SCR/TWI/CIR/SPI
 * 
 * Register  Offset       Description
 * Pn_CFG0   n*0x24+0x00  Port n Configure Register 0 (n=2,3,5,6,7) Pn0..7_CFG (3+1 bits per GPIO)
 * Pn_CFG1   n*0x24+0x04  Port n Configure Register 1 (n=2,3,5,6,7) Pn8..15_CFG (3+1 bits per GPIO)
 * Pn_CFG2   n*0x24+0x08  Port n Configure Register 2 (n=2,3,5,6,7) Pn16..23_CFG (3+1 bits per GPIO)
 * Pn_CFG3   n*0x24+0x0c  Port n Configure Register 3 (n=2,3,5,6,7) Pn24..31_CFG (3+1 bits per GPIO)
 * Pn_DAT    n*0x24+0x10  Port n Data Register (n=2,3,5,6,7) Pn_DAT (1 bit per GPIO)
 * Pn_DRV0   n*0x24+0x14  Port n Multi-Driving Register 0 (n=2,3,5,6,7) Pn0..15_DRV (2 bits per GPIO)
 * Pn_DRV1   n*0x24+0x18  Port n Multi-Driving Register 1 (n=2,3,5,6,7) Pn16..31_DRV (2 bits per GPIO)
 * Pn_PUL0   n*0x24+0x1c  Port n Pull Register 0 (n=2,3,5,6,7) Pn0..15_PULL (2 bits per GPIO)
 * Pn_PUL1   n*0x24+0x20  Port n Pull Register 1 (n=2,3,5,6,7) Pn16..31_PULL
 * 
 * Page 433 User Manual
 * CPUS_PORT PIO base address 0x0702_2000
 * 2 ports (PL, PM)
 * 
 * Port Name  Bank  Num Pins  Multiplex
 * PL         11    11        Input/Output
 * PM         12    5         Input/Output
 * 
 * Register offsets as per CPUX_PORT
 */
public class AllwinnerH6MmapGpio extends AllwinnerHMmapGpio {
	private static final long GPIOA_BASE = 0x0300_B000L;
	private static final int GPIOA_INT_OFFSET = 0;
	private static final long GPIOL_BASE = 0x0702_2000L;
	private static final int GPIOL_INT_OFFSET = 0;

	private static final int[] NUM_GPIOS_BY_BANK = { 0, 0, 17, 27, 0, 7, 15, 11, 0, 0, 0, 11, 5 };

	public AllwinnerH6MmapGpio() {
		super(GPIOA_BASE, GPIOA_INT_OFFSET, GPIOL_BASE, GPIOL_INT_OFFSET, NUM_GPIOS_BY_BANK);
	}

	@Override
	void initialiseGpioModes() {
		// PC0 (64)
		addGpioMode(64, 4, DeviceMode.SPI);
		// PC2 (66)
		addGpioMode(66, 4, DeviceMode.SPI);
		// PC3 (67)
		addGpioMode(67, 4, DeviceMode.SPI);
		// PC5 (69)
		addGpioMode(69, 4, DeviceMode.SPI);

		// PD19 (115)
		addGpioMode(115, 4, DeviceMode.SERIAL);
		// PD20 (116)
		addGpioMode(116, 4, DeviceMode.SERIAL);
		// PD21 (117)
		addGpioMode(117, 4, DeviceMode.SERIAL);
		// PD22 (118)
		addGpioMode(118, 2, DeviceMode.PWM_OUTPUT);
		addGpioMode(118, 4, DeviceMode.SERIAL);
		// PD23 (119)
		addGpioMode(119, 2, DeviceMode.I2C);
		addGpioMode(119, 4, DeviceMode.SERIAL);
		// PD24 (120)
		addGpioMode(120, 2, DeviceMode.I2C);
		addGpioMode(120, 4, DeviceMode.SERIAL);
		// PD25 (121)
		addGpioMode(121, 2, DeviceMode.I2C);
		addGpioMode(121, 4, DeviceMode.SERIAL);
		// PD26 (122)
		addGpioMode(122, 2, DeviceMode.I2C);
		addGpioMode(122, 4, DeviceMode.SERIAL);

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

		// PH0 (224)
		addGpioMode(224, 2, DeviceMode.SERIAL);
		// PH1 (225)
		addGpioMode(225, 2, DeviceMode.SERIAL);
		// PH3 (227)
		addGpioMode(227, 2, DeviceMode.SPI);
		// PH4 (228)
		addGpioMode(228, 2, DeviceMode.SPI);
		// PH5 (229)
		addGpioMode(229, 2, DeviceMode.SPI);
		addGpioMode(229, 4, DeviceMode.I2C);
		// PH6 (230)
		addGpioMode(230, 2, DeviceMode.SPI);
		addGpioMode(230, 4, DeviceMode.I2C);

		// PL0 (352)
		addGpioMode(352, 3, DeviceMode.I2C);
		// PL1 (353)
		addGpioMode(353, 3, DeviceMode.I2C);
		// PL2 (354)
		addGpioMode(354, 2, DeviceMode.SERIAL);
		// PL3 (355)
		addGpioMode(355, 2, DeviceMode.SERIAL);
		// PL8 (360)
		addGpioMode(360, 2, DeviceMode.PWM_OUTPUT);
		// PL10 (362)
		addGpioMode(362, 3, DeviceMode.PWM_OUTPUT);
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		int[] gpios = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 16, 20, 24, 28, 31, 32, 63, 64, 80, 96, 112, 128, 144, 228, 351,
				352, 355, 384, //
				// Orange Pi 3 LTS
				111, 112, 114, 117, 118, 119, 120, 121, 122, 227, 228, 229, 230, 354, 355, 360, 362, //
				// Orange Pi One+
				64, 66, 67, 69, 71, 72, 73, 117, 118, 119, 120, 121, 122, 227, 228, 229, 230 //
		};

		// Valid banks - 2, 3, 5, 6, 7, 11, 12
		final String[] bank_letters = { "Z", "Z", "C", "D", "Z", "F", "G", "H", "Z", "Z", "Z", "L", "M" };
		for (int gpio : gpios) {
			int bank = gpio >> 5;
			int bank_index = gpio % 32;
			int mode_shift = (gpio % 8) << 2;
			int data_shift = bank_index;
			int pud_shift = (gpio % 16) << 1;
			boolean valid_gpio = bank_index < NUM_GPIOS_BY_BANK[bank];

			int mode_int_offset;
			int data_int_offset;
			int pud_int_offset;
			long phyaddr;
			long my_phyaddr;
			if (bank < 11) {
				mode_int_offset = CONFIG_REG_INT_OFFSET + bank * BANK_INT_OFFSET + (bank_index >> 3);
				data_int_offset = DATA_REG_INT_OFFSET + bank * BANK_INT_OFFSET;
				pud_int_offset = PULL_REG_INT_OFFSET + bank * BANK_INT_OFFSET + (bank_index >> 4);

				phyaddr = GPIOA_BASE + (bank * BANK_INT_OFFSET * 4) + DATA_REG_INT_OFFSET * 4;
				my_phyaddr = GPIOA_BASE + data_int_offset * 4;
			} else {
				mode_int_offset = CONFIG_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET + (bank_index >> 3);
				data_int_offset = DATA_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET;
				pud_int_offset = PULL_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET + (bank_index >> 4);

				phyaddr = GPIOL_BASE + ((bank - 11) * BANK_INT_OFFSET * 4) + DATA_REG_INT_OFFSET * 4;
				my_phyaddr = GPIOL_BASE + data_int_offset * 4;
			}
			System.out.format(
					"gpio: %3d, name: %s, bank: %2d, bank_index: %2d, valid: %5b, phyaddr: 0x%08x, my_phyaddr: 0x%08x, mode_shift: %2d, mode_offset: 0x%03x, data_shift: %2d, data_offset: 0x%03x, pud_shift: %2d, pud_offset: 0x%03x%n",
					gpio,
					(bank_letters[bank] == "Z" ? "Z" : "P") + bank_letters[bank] + String.format("%02d", gpio % 32),
					bank, bank_index, valid_gpio, phyaddr, my_phyaddr, mode_shift, mode_int_offset * 4, data_shift,
					data_int_offset * 4, pud_shift, pud_int_offset * 4);
		}

		if (args.length < 1) {
			System.out.println("Usage: <gpio>");
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);

		try (AllwinnerH6MmapGpio mmap_gpio = new AllwinnerH6MmapGpio()) {
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
