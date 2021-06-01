package com.diozero.internal.board.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerH6MmapGpio.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

/**
 * https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/wiringPi.c
 * https://github.com/orangepi-xunlong/wiringOP/blob/master/wiringPi/OrangePi.h#L70
 * https://linux-sunxi.org/images/4/46/Allwinner_H6_V200_User_Manual_V1.1.pdf
 * https://linux-sunxi.org/images/5/5c/Allwinner_H6_V200_Datasheet_V1.1.pdf
 */
public class AllwinnerH6MmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/mem";

	private static final int MEM_INFO = 1024;
	private static final int BLOCK_SIZE = 4 * MEM_INFO;

	private static final int GPIOA_BASE = 0x0300B000;
	private static final int GPIOA_OFFSET = 0x0;
	private static final int GPIOA_INT_OFFSET = GPIOA_OFFSET / 4;
	// private static final int GPIOA_BASE_MAP = GPIOA_BASE + GPIOA_OFFSET;
	private static final int MAP_SIZE_A = BLOCK_SIZE;

	/*-
	private static final int GPIOL_BASE = 0x07022000;
	private static final int GPIOL_OFFSET = 0x0;
	private static final int GPIOL_INT_OFFSET = GPIOL_OFFSET / 4;
	// private static final int GPIOL_BASE_MAP = GPIOL_BASE + GPIOL_OFFSET;
	private static final int MAP_SIZE_L = BLOCK_SIZE * 2;
	 */

	private boolean initialised;
	private MmapIntBuffer gpioAMmapIntBuffer;

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			gpioAMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, GPIOA_BASE, MAP_SIZE_A, ByteOrder.LITTLE_ENDIAN);

			initialised = true;
		}
	}

	@Override
	public synchronized void close() {
		if (initialised) {
			gpioAMmapIntBuffer.close();
			gpioAMmapIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		/*-
		int bank = gpio >> 5;
		int index = gpio - (bank << 5);
		int int_offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
		int shift = ((index - ((index >> 3) << 3)) << 2);
		 */
		int int_offset = (gpio >> 3) + 5 * (gpio >> 5);
		int shift = (gpio % 8) * 4;

		int mode_val = (gpioAMmapIntBuffer.get(GPIOA_INT_OFFSET + int_offset) >> shift) & 0b111;
		DeviceMode mode;
		switch (mode_val) {
		case 0:
			mode = DeviceMode.DIGITAL_INPUT;
			break;
		case 1:
			mode = DeviceMode.DIGITAL_OUTPUT;
			break;
		default:
			mode = DeviceMode.UNKNOWN;
		}

		return mode;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		/*-
		int bank = gpio >> 5;
		int index = gpio - (bank << 5);
		int int_offset = ((bank * 36) + ((index >> 3) << 2)) / 4;
		int shift = ((index - ((index >> 3) << 3)) << 2);
		 */
		int int_offset = (gpio >> 3) + 5 * (gpio >> 5);
		int shift = (gpio % 8) * 4;

		int phyaddr = GPIOA_INT_OFFSET + int_offset;

		int reg_val = gpioAMmapIntBuffer.get(phyaddr);

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

		gpioAMmapIntBuffer.put(phyaddr, reg_val);
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
	}

	@Override
	public boolean gpioRead(int gpio) {
		/*-
		int bank = gpio >> 5;
		int int_offset = ((bank * 36) + 0x10) / 4;
		int shift = gpio - (bank << 5);
		 */
		int int_offset = (gpio >> 5) * 9 + 4;
		int shift = gpio % 32;

		return ((gpioAMmapIntBuffer.get(GPIOA_INT_OFFSET + int_offset) >> shift) & 1) == 1;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		/*-
		int bank = gpio >> 5;
		int int_offset = ((bank * 36) + 0x10) / 4;
		int shift = gpio - (bank << 5);
		 */
		int int_offset = (gpio >> 5) * 9 + 4;
		int shift = gpio % 32;

		int addr_int_offset = GPIOA_INT_OFFSET + int_offset;

		int reg_val = gpioAMmapIntBuffer.get(addr_int_offset);

		if (value) {
			reg_val |= (1 << shift);
		} else {
			reg_val &= ~(1 << shift);
		}

		gpioAMmapIntBuffer.put(addr_int_offset, reg_val);
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
