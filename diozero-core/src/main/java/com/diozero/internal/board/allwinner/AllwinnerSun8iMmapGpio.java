package com.diozero.internal.board.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     AllwinnerSun8iMmapGpio.java  
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

import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/**
 * See https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/wiringPi.c
 */
public class AllwinnerSun8iMmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/mem";
	private static final int PAGE_SIZE = 4 * 1024;
	private static final int BLOCK_SIZE = 6 * 1024;
	private static final int GPIO_BASE_BP = 0x01C20000;
	private static final int SUNXI_GPIO_INT_OFFSET = 0x800 / 4;

	private boolean initialised;
	private MmapIntBuffer mmapIntBuffer;

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			mmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, GPIO_BASE_BP, BLOCK_SIZE * 10, ByteOrder.LITTLE_ENDIAN);

			initialised = true;
		}
	}

	@Override
	public synchronized void close() {
		if (initialised) {
			mmapIntBuffer.close();
			mmapIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		/*-
		 * int bank = gpio >> 5;
		 * int index = gpio - (bank << 5);
		 * int int_offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
		 * int shift = ((index - ((index >> 3) << 3)) << 2);
		 */
		int int_offset = (gpio / 8) + 5 * (gpio / 32);
		int shift = (gpio % 8) * 4;

		// phyaddr = SUNXI_GPIO_BASE + (bank * 36) + ((index >> 3) << 2);
		int phyaddr = SUNXI_GPIO_INT_OFFSET + int_offset;

		int mode_val = ((mmapIntBuffer.get(phyaddr) >> shift) & 7);
		switch (mode_val) {
		case 0:
			return DeviceMode.DIGITAL_INPUT;
		case 1:
			return DeviceMode.DIGITAL_OUTPUT;
		default:
			return DeviceMode.UNKNOWN;
		}
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		/*-
		 * int bank = gpio >> 5;
		 * int index = gpio - (bank << 5);
		 * int int_offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
		 * int shift = ((index - ((index >> 3) << 3)) << 2);
		 */
		int int_offset = (gpio / 8) + 5 * (gpio / 32);
		int shift = (gpio % 8) * 4;

		// phyaddr = SUNXI_GPIO_BASE + (bank * 36) + ((index >> 3) << 2);
		int phyaddr = SUNXI_GPIO_INT_OFFSET + int_offset;

		int reg_val = mmapIntBuffer.get(phyaddr);

		switch (mode) {
		case DIGITAL_INPUT:
			reg_val &= ~(7 << shift);
			break;
		case DIGITAL_OUTPUT:
			reg_val &= ~(7 << shift);
			reg_val |= (1 << shift);
			break;
		case PWM_OUTPUT:
			Logger.warn("Mode {} not yet implemented for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		default:
			Logger.warn("Invalid mode ({}) for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		}

		mmapIntBuffer.put(phyaddr, reg_val);
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		/*-
		 * int bank = gpio >> 5;
		 * int index = gpio - (bank << 5);
		 * int sub = index >> 4;
		 * int sub_index = index - 16 * sub;
		 * int int_offset = ((bank * 36) + 0x1c + sub * 4) / 4;
		 */

		// FIXME There must be a simpler formula...
		int int_offset = (((gpio / 32) * 36) + 0x1c + ((gpio / 16) % 2) * 4) / 4;
		int shift = gpio % 16;

		// phyaddr = SUNXI_GPIO_BASE + (bank * 36) + 0x1c + sub * 4; // +0x10 ->
		// pullUpDn reg
		int phyaddr = SUNXI_GPIO_INT_OFFSET + int_offset;

		// #define PUD_OFF 0
		// #define PUD_DOWN 1
		// #define PUD_UP 2
		// upDnConvert[3] = {7, 7, 5};
		int pud_val;
		switch (pud) {
		case PULL_UP:
			pud_val = 5;
			break;
		case PULL_DOWN:
			pud_val = 7;
			break;
		case NONE:
		default:
			pud_val = 7;
		}

		int reg_val = mmapIntBuffer.get(phyaddr);
		reg_val &= ~(3 << (shift << 1));
		reg_val |= (pud_val << (shift << 1));
		mmapIntBuffer.put(phyaddr, reg_val);

		SleepUtil.sleepMillis(1);
	}

	@Override
	public boolean gpioRead(int gpio) {
		/*-
		 * int bank = gpio >> 5;
		 * int int_offset = ((bank * 36) + 0x10) / 4;
		 * int shift = gpio - (bank << 5);
		 */
		int int_offset = (gpio / 32) * 9 + 4;
		int shift = gpio % 32;

		// int phyaddr = SUNXI_GPIO_BASE + (bank * 36) + 0x10; // +0x10 -> data reg
		int phyaddr = SUNXI_GPIO_INT_OFFSET + int_offset;

		return ((mmapIntBuffer.get(phyaddr) >> shift) & 1) == 1;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		/*-
		 * int bank = gpio >> 5;
		 * int int_offset = ((bank * 36) + 0x10) / 4;
		 * int shift = gpio - (bank << 5);
		 */
		int int_offset = (gpio / 32) * 9 + 4;
		int shift = gpio % 32;

		// int phyaddr = SUNXI_GPIO_BASE + (bank * 36) + 0x10; // +0x10 -> data reg
		int addr_int_offset = SUNXI_GPIO_INT_OFFSET + int_offset;

		int reg_val = mmapIntBuffer.get(addr_int_offset);

		if (value) {
			reg_val |= (1 << shift);
		} else {
			reg_val &= ~(1 << shift);
		}

		mmapIntBuffer.put(addr_int_offset, reg_val);
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		// Computed values for GPIO mode register
		for (int gpio = 0; gpio < 224; gpio++) {
			int bank = gpio >> 5;
			int index = gpio - (bank << 5);
			int offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
			int shift = ((index - ((index >> 3) << 3)) << 2);

			int my_bank = gpio / 32;
			int my_index = gpio % 32;
			int my_offset = (gpio / 8) + 5 * (gpio / 32);
			int my_shift = (gpio % 8) * 4;

			if (bank != my_bank || index != my_index || offset != my_offset || shift != my_shift) {
				System.out.format("Xx Mode for GPIO %d - bank: %d, index:%d, offset: %d, shift: %d%n", gpio, bank,
						index, offset, shift);
				System.out.format("My Mode for GPIO %d - bank: %d, index:%d, offset: %d, shift: %d%n", gpio, my_bank,
						my_index, my_offset, my_shift);
			}
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
				System.out.format("Xx Value for GPIO %d - bank: %d, offset: %d, shift: %d%n", gpio, bank,
						gpio_int_offset, shift);
				System.out.format("My Value for GPIO %d - bank: %d, offset: %d, shift: %d%n", gpio, my_bank, my_offset,
						my_shift);
			}
		}

		// Computed values for GPIO pull up/down control
		for (int gpio = 0; gpio < 224; gpio++) {
			int bank = gpio >> 5;
			int index = gpio - (bank << 5);
			int sub = index >> 4;
			int sub_index = index - 16 * sub;
			int int_offset = ((bank * 36) + 0x1c + sub * 4) / 4;

			int my_bank = gpio / 32;
			int my_index = gpio % 32;
			int my_sub = (gpio / 16) % 2;
			int my_sub_index = gpio % 16;
			int my_int_offset = (((gpio / 32) * 36) + 0x1c + ((gpio / 16) % 2) * 4) / 4;

			// System.out.format("Xx Value for GPIO PUD %d - bank: %d, index: %d, sub: %d,
			// sub_index: %d, int_offset=%d%n",
			// gpio, bank, index, sub, sub_index, int_offset);
			if (bank != my_bank || index != my_index || sub != my_sub || sub_index != my_sub_index
					|| int_offset != my_int_offset) {
				System.out.format(
						"Xx Value for GPIO PUD %d - bank: %d, index: %d, sub: %d, sub_index: %d, int_offset=%d%n", gpio,
						bank, index, sub, sub_index, int_offset);
				System.out.format(
						"My Value for GPIO PUD %d - bank: %d, index: %d, sub: %d, sub_index: %d, int_offset=%d%n", gpio,
						my_bank, my_index, my_sub, my_sub_index, my_int_offset);
			}
		}

		int gpio = Integer.parseInt(args[0]);

		try (AllwinnerSun8iMmapGpio mmap_gpio = new AllwinnerSun8iMmapGpio()) {
			mmap_gpio.initialise();
			System.out.println("Mode: " + mmap_gpio.getMode(gpio));
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
				long start = System.currentTimeMillis();
				for (int j = 0; j < iterations; j++) {
					mmap_gpio.gpioWrite(gpio, true);
					mmap_gpio.gpioWrite(gpio, false);
				}
				long duration = System.currentTimeMillis() - start;
				double frequency = iterations / (duration / 1000.0);
				System.out.println("Took " + duration + " ms for " + iterations + ", frequency " + frequency + " Hz");
			}
		}
	}
}
