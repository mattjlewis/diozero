package com.diozero.internal.soc.rockchip;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     RK3588Conversions.java
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RK3588Conversions {
	public static void main(String[] args) {
		for (String gpio_name : new String[] { "GPIO0_A0", "GPIO0_B0", "GPIO0_C0", "GPIO0_D0", "GPIO0_A1", "GPIO0_A2",
				"GPIO0_A3", "GPIO0_A4", "GPIO0_A5", "GPIO0_A6", "GPIO0_A7", "GPIO0_D5", "GPIO1_B6", "GPIO1_C6",
				"GPIO2_D4", "GPIO4_A3", "GPIO4_B2" }) {
			check(gpio_name);
		}
	}

	@SuppressWarnings("boxing")
	private static void check(String gpioName) {
		Matcher m = Pattern.compile("GPIO(\\d)_(\\w)(\\d)").matcher(gpioName);
		if (!m.matches()) {
			throw new IllegalArgumentException();
		}

		int x_bank = Integer.parseInt(m.group(1));
		int x_group = m.group(2).charAt(0) - 'A';
		int x_index = Integer.parseInt(m.group(3));
		int pin = x_bank * 32 + x_group * 8 + x_index;

		{
			int bank = pin >> 5;
			int index = pin - (bank << 5);
			int bus_ioc_shift = ((index % 4) << 2);
			int bus_ioc_byte_offset = (0x20 * bank) + ((index >> 2) << 2);
			int ddr_shift = index % 16;
			int ddr_byte_offset = RK3588MmapGpio.GPIO_SWPORT_DDR * 4 + ((index / 16) << 2);
			long bus_ioc_phyaddr = RK3588MmapGpio.BUS_IOC_BASE + bus_ioc_byte_offset;
			long ddr_phyaddr = RK3588MmapGpio.GPIO_BASE[bank] + ddr_byte_offset;
			System.out.format(
					"GPIO: %s, gpio: %3d, bank: %d, index: %2d,       bus_ioc_shift: %2d, bus_ioc_byte_offset: %3d, bus_ioc_phyaddr: 0x%x, ddr_shift: %2d, ddr_byte_offset: %2d, ddr_phyaddr: 0x%x%n",
					gpioName, pin, bank, index, bus_ioc_shift, bus_ioc_byte_offset, bus_ioc_phyaddr, ddr_shift,
					ddr_byte_offset, ddr_phyaddr);
		}

		{
			final int gpio = pin;
			final int bank = gpio >> 5;
			final int bank_offset = gpio % 32;
			final int bus_ioc_shift = (gpio % 4) << 2;
			final int bus_ioc_int_offset = (bank << 3) + (bank_offset >> 2);
			final int ddr_shift = gpio % 16;
			final int ddr_int_offset = RK3588MmapGpio.GPIO_SWPORT_DDR + (bank_offset >> 4);
			final long bus_ioc_phyaddr = RK3588MmapGpio.BUS_IOC_BASE + bus_ioc_int_offset * 4;
			final long ddr_phyaddr = RK3588MmapGpio.GPIO_BASE[bank] + ddr_int_offset * 4;
			System.out.format(
					"GPIO: %s, gpio: %3d, bank: %d, bank_offset: %2d, bus_ioc_shift: %2d, bus_ioc_byte_offset: %3d, bus_ioc_phyaddr: 0x%x, ddr_shift: %2d, ddr_byte_offset: %2d, ddr_phyaddr: 0x%x%n",
					gpioName, gpio, bank, bank_offset, bus_ioc_shift, bus_ioc_int_offset * 4, bus_ioc_phyaddr,
					ddr_shift, ddr_int_offset * 4, ddr_phyaddr);
		}
	}
}
