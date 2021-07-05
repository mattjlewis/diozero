package com.diozero.internal.board.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerSun8iMmapGpio.java
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
import com.diozero.util.SleepUtil;

/**
 * https://wiki.friendlyarm.com/wiki/images/4/4b/Allwinner_H3_Datasheet_V1.2.pdf
 * See https://github.com/friendlyarm/WiringNP/blob/master/wiringPi/wiringPi.c
 *
 *
 * Banks of GPIOs A-L:
 *
 * A=0-31, B=32-63, C=64-95, D=96-127, E=128-159, F=160-192, G=192-223,
 * H=224-255, I=256-287, J=288-319, K=320-351, L=352-383
 */
public class AllwinnerSun8iMmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/mem";

	private static final int MEM_INFO = 1024;
	private static final int PAGE_SIZE = 4 * MEM_INFO;
	private static final int BLOCK_SIZE = 6 * MEM_INFO;

	private static final int GPIOA_BASE = 0x01C20000;
	private static final int GPIOA_OFFSET = 0x800;
	private static final int GPIOA_INT_OFFSET = GPIOA_OFFSET / 4;
	private static final int MAP_SIZE_A = BLOCK_SIZE * 10;

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
		int shift = (gpio % 8) << 2;

		int mode_val = (gpioAMmapIntBuffer.get(GPIOA_INT_OFFSET + int_offset) >> shift) & 0b111;
		DeviceMode mode;
		switch (mode_val) {
		case 0:
			mode = DeviceMode.DIGITAL_INPUT;
			break;
		case 1:
			mode = DeviceMode.DIGITAL_OUTPUT;
			break;
		case 2:
			if (gpio == 11 || gpio == 12) {
				mode = DeviceMode.I2C;
			} else if (gpio >= 0 && gpio < 6 || gpio >= 198 && gpio < 202) {
				mode = DeviceMode.SERIAL;
			} else {
				mode = DeviceMode.UNKNOWN;
			}
			break;
		case 3:
			if (gpio == 5) {
				mode = DeviceMode.PWM_OUTPUT;
			} else if (gpio >= 64 && gpio < 68) {
				mode = DeviceMode.SPI;
			} else {
				mode = DeviceMode.UNKNOWN;
			}
			break;
		case 7:
			// I/O Disable
			mode = DeviceMode.UNKNOWN;
			break;
		default:
			Logger.warn("Unknown mode for gpio {}: {}", Integer.valueOf(gpio), Integer.valueOf(mode_val));
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
		int shift = (gpio % 8) << 2;

		int phyaddr = GPIOA_INT_OFFSET + int_offset;

		int reg_val = gpioAMmapIntBuffer.get(phyaddr);
		int mode_val;

		switch (mode) {
		case DIGITAL_INPUT:
			mode_val = 0b000;
			break;
		case DIGITAL_OUTPUT:
			mode_val = 0b001;
			break;
		case PWM_OUTPUT:
			if (gpio == 5) {
				mode_val = 0b011;
			} else {
				Logger.warn("Invalid mode ({}) for GPIO #{}", mode, Integer.valueOf(gpio));
				return;
			}
			break;
		default:
			Logger.warn("Invalid mode ({}) for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		}

		reg_val &= ~(0b111 << shift);
		reg_val |= (mode_val << shift);

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

		/*-
		 * Pull Registers
		 * 00: Disable, 01: Pull-up, 10: Pull-down
		 *
		 * Bank   Offset R0    Offset R1    GPIOs
		 * PA (0) 0x1c (0-15), 0x2d (16-31) 0..31
		 * PB (1)                           32..63
		 * PC (2) 0x64 (0-15), 0x68 (16-31) 64..95
		 * PD (3) 0x88 (0-15), 0x8c (16-31) 96..127
		 * PE (4) 0xac (0-15), 0xb0 (16-31) 128..159
		 * PF (5) 0xd0 (0-15), 0xd4 (16-31) 160..191
		 * PG (6) 0xf4 (0-15), 0xf8 (16-31) 192..223
		 * PH (7)                           224..255
		 * PI (8)                           256..287
		 * PJ (9)                           288..319
		 * PK (a)                           320..351
		 * PL (b) 0x1c (0-15), 0x20 (16-31) 352..383
		 */
		int bank = gpio >> 5; // equivalent to / 32
		int reg = (gpio >> 4) % 2; // Register 0 or 1
		int shift = (gpio % 16) << 1; // Shift 0..30
		int int_offset = (0x1c + bank * 0x24) / 4 + reg;

		int phyaddr = GPIOA_INT_OFFSET + int_offset;

		int pud_val;
		switch (pud) {
		case PULL_UP:
			pud_val = 0b01;
			break;
		case PULL_DOWN:
			pud_val = 0b10;
			break;
		case NONE:
		default:
			pud_val = 0b00;
		}

		int reg_val = gpioAMmapIntBuffer.get(phyaddr);
		reg_val &= ~(0b11 << shift);
		reg_val |= (pud_val << shift);

		gpioAMmapIntBuffer.put(phyaddr, reg_val);

		SleepUtil.sleepMillis(1);
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
			System.out.println("Usage: " + AllwinnerSun8iMmapGpio.class.getName() + " <gpio>");
			return;
		}
		int gpio = Integer.parseInt(args[0]);

		try (AllwinnerSun8iMmapGpio mmap_gpio = new AllwinnerSun8iMmapGpio()) {
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
