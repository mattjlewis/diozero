package com.diozero.internal.board.soc.amlogic;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OdroidN2PlusMmapGpio.java
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InvalidModeException;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.Hex;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

public class AmlogicS922XMmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";

	/*
	 * The actual base address of the register is 0xFF634400. However, that is not
	 * aligned with the page boundaries. In order to deal with this, we use the
	 * previous page boundary (0xFF634000). Since we are dealing with 32-bit ints,
	 * we make up the 0x400 byte difference by adding 0x100 to each of the offsets.
	 * Therefore, none of these match the documented addresses or offsets, but they
	 * all actually work.
	 */
	private static final long J2_GPIO_BASE_ADDRESS = 0xFF63_4000L;
	private static final int BLOCK_SIZE = 4096;

	private static final int J2_GPIO_START = 410;

	private static final int J2_GPIOA_PIN_START = J2_GPIO_START + 50;
	private static final int J2_GPIOA_PIN_END = J2_GPIO_START + 65;

	private static final int J2_GPIOX_PIN_START = J2_GPIO_START + 66;
	private static final int J2_GPIOX_PIN_END = J2_GPIO_START + 85;

	// GPIOA offsets
	private static final int J2_GPIOA_OEN_REG_OFFSET = 0x120;
	private static final int J2_GPIOA_OUT_REG_OFFSET = 0x121;
	private static final int J2_GPIOA_INP_REG_OFFSET = 0x122;
	private static final int J2_GPIOA_PUEN_REG_OFFSET = 0x14D;
	private static final int J2_GPIOA_PUPD_REG_OFFSET = 0x13F;

	// GPIOX offsets
	private static final int J2_GPIOX_OEN_REG_OFFSET = 0x116;
	private static final int J2_GPIOX_OUT_REG_OFFSET = 0x117;
	private static final int J2_GPIOX_INP_REG_OFFSET = 0x118;
	private static final int J2_GPIOX_PUEN_REG_OFFSET = 0x14A;
	private static final int J2_GPIOX_PUPD_REG_OFFSET = 0x13C;

	private boolean initialised;
	private MmapIntBuffer j2MappedByteBuffer;

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			j2MappedByteBuffer = new MmapIntBuffer(MEM_DEVICE, J2_GPIO_BASE_ADDRESS, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);

			initialised = true;
		}
	}

	@Override
	public synchronized void close() {
		if (initialised) {
			j2MappedByteBuffer.close();

			initialised = false;
		}
	}

	/*
	 * Offset to the GPIO Output Enable register
	 */
	private static int gpioToOutputEnableRegOffset(int gpio) {
		if (gpio >= J2_GPIOA_PIN_START && gpio <= J2_GPIOA_PIN_END) {
			return J2_GPIOA_OEN_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_OEN_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Output register
	 */
	private static int gpioToOutputRegOffset(int gpio) {
		if (gpio >= J2_GPIOA_PIN_START && gpio <= J2_GPIOA_PIN_END) {
			return J2_GPIOA_OUT_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_OUT_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Input register
	 */
	private static int gpioToInputRegOffset(int gpio) {
		if (gpio >= J2_GPIOA_PIN_START && gpio <= J2_GPIOA_PIN_END) {
			return J2_GPIOA_INP_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_INP_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Pull up/down enable register
	 */
	private static int gpioToPullUpDownEnableRegOffset(int gpio) {
		if (gpio >= J2_GPIOA_PIN_START && gpio <= J2_GPIOA_PIN_END) {
			return J2_GPIOA_PUEN_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_PUEN_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Pull up/down register
	 */
	private static int gpioToPullUpDownRegOffset(int gpio) {
		if (gpio >= J2_GPIOA_PIN_START && gpio <= J2_GPIOA_PIN_END) {
			return J2_GPIOA_PUPD_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_PUPD_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO bit
	 */
	private static int gpioToRegShiftBit(int gpio) {
		if (gpio >= J2_GPIOA_PIN_START && gpio <= J2_GPIOA_PIN_END) {
			return gpio - J2_GPIOA_PIN_START;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return gpio - J2_GPIOX_PIN_START;
		}
		return -1;
	}

	private MmapIntBuffer getIntBuffer() {
		if (!initialised) {
			throw new IllegalStateException("Memory map is not initialized");
		}
		return j2MappedByteBuffer;
	}

	@Override
	public DeviceMode getMode(int gpio) {
		int fsel = gpioToOutputEnableRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);

		if (fsel == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}

		MmapIntBuffer mmap_int_buffer = getIntBuffer();
		if ((mmap_int_buffer.get(fsel) & 1 << shift) == 0) {
			return DeviceMode.DIGITAL_OUTPUT;
		}

		return DeviceMode.DIGITAL_INPUT;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			// mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) | (1 << shift));
			setModeUnchecked(gpio, 1);
			break;
		case DIGITAL_OUTPUT:
			// mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) & ~(1 << shift));
			setModeUnchecked(gpio, 0);
			break;
		default:
			throw new InvalidModeException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}

	@Override
	public void setModeUnchecked(int gpio, int mode) {
		int fsel = gpioToOutputEnableRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);

		if (fsel == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return;
		}

		MmapIntBuffer mmap_int_buffer = getIntBuffer();
		// Mode can only be 0 or 1
		if (mode == 0) {
			mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) & ~(1 << shift));
		} else {
			mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) | (1 << shift));
		}
	}

	@Override
	public Optional<GpioPullUpDown> getPullUpDown(int gpio) {
		int puen = gpioToPullUpDownEnableRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);

		if (puen == -1 || shift == -1) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio);
		}

		MmapIntBuffer mmap_int_buffer = getIntBuffer();
		if ((mmap_int_buffer.get(puen) & (1 << shift)) != 0) {
			int pupd = gpioToPullUpDownRegOffset(gpio);
			return Optional.of((mmap_int_buffer.get(pupd) & (1 << shift)) == 0 ? GpioPullUpDown.PULL_DOWN
					: GpioPullUpDown.PULL_UP);
		}
		return Optional.of(GpioPullUpDown.NONE);
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		int shift = gpioToRegShiftBit(gpio);
		int puen = gpioToPullUpDownEnableRegOffset(gpio);

		if (puen == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return;
		}

		MmapIntBuffer mmap_int_buffer = getIntBuffer();
		if (pud == GpioPullUpDown.NONE) {
			mmap_int_buffer.put(puen, mmap_int_buffer.get(puen) & ~(1 << shift));
		} else {
			mmap_int_buffer.put(puen, mmap_int_buffer.get(puen) | (1 << shift));

			int pupd = gpioToPullUpDownRegOffset(gpio);
			if (pud == GpioPullUpDown.PULL_UP) {
				mmap_int_buffer.put(pupd, mmap_int_buffer.get(pupd) | (1 << shift));
			} else {
				mmap_int_buffer.put(pupd, mmap_int_buffer.get(pupd) & ~(1 << shift));
			}
		}
	}

	@Override
	public boolean gpioRead(int gpio) {
		int reg = gpioToInputRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);

		if (reg == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return false;
		}

		MmapIntBuffer mmap_int_buffer = getIntBuffer();
		return (mmap_int_buffer.get(reg) & (1 << shift)) != 0;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		// Note no boundary checks to maximise performance

		int reg = gpioToOutputRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);

		MmapIntBuffer mmap_int_buffer = getIntBuffer();
		if (value) {
			mmap_int_buffer.put(reg, mmap_int_buffer.get(reg) | (1 << shift));
		} else {
			mmap_int_buffer.put(reg, mmap_int_buffer.get(reg) & ~(1 << shift));
		}
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		System.out.println(ByteOrder.nativeOrder());
		if (args.length != 2) {
			System.out.println("Usage: " + AmlogicS922XMmapGpio.class.getName() + " <gpio> <iterations>");
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);
		int iterations = Integer.parseInt(args[1]);

		int gp_set_reg = gpio < J2_GPIOX_PIN_START ? J2_GPIOA_OUT_REG_OFFSET : J2_GPIOX_OUT_REG_OFFSET;
		int gp_set_reg2 = gpioToOutputRegOffset(gpio);
		System.out.println("gp_set_reg=" + gp_set_reg + ", gp_set_reg2=" + gp_set_reg2);
		int gp_lev_reg = gpio < J2_GPIOX_PIN_START ? J2_GPIOA_INP_REG_OFFSET : J2_GPIOX_INP_REG_OFFSET;
		int gp_lev_reg2 = gpioToInputRegOffset(gpio);
		System.out.println("gp_lev_reg=" + gp_lev_reg + ", gp_lev_reg2=" + gp_lev_reg2);
		int gp_fsel_reg = gpio < J2_GPIOX_PIN_START ? J2_GPIOA_OEN_REG_OFFSET : J2_GPIOX_OEN_REG_OFFSET;
		int gp_fsel_reg2 = gpioToOutputEnableRegOffset(gpio);
		System.out.println("gp_fsel_reg=" + gp_fsel_reg + ", gp_fsel_reg2=" + gp_fsel_reg2);

		System.out.format("gpioToGPSETReg(%d)=0x%04x%n", 214, gpioToOutputRegOffset(214));
		System.out.format("gpioToGPSETReg(%d)=0x%04x%n", 219, gpioToOutputRegOffset(219));
		System.out.format("gpioToGPFSELReg(%d)=0x%04x%n", 214, gpioToOutputEnableRegOffset(214));
		System.out.format("gpioToGPFSELReg(%d)=0x%04x%n", 219, gpioToOutputEnableRegOffset(219));

		try (AmlogicS905MmapGpio mmap_gpio = new AmlogicS905MmapGpio()) {
			mmap_gpio.initialise();

			System.out.println("getMode(" + gpio + ")=" + mmap_gpio.getMode(gpio));
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			System.out.println("getMode(" + gpio + ")=" + mmap_gpio.getMode(gpio));

			System.out.println("Current val=" + mmap_gpio.gpioRead(gpio));
			for (int i = 0; i < 5; i++) {
				System.out.println("on");
				mmap_gpio.gpioWrite(gpio, true);
				System.out.println("Current val=" + mmap_gpio.gpioRead(gpio));
				SleepUtil.sleepSeconds(1);
				System.out.println("off");
				mmap_gpio.gpioWrite(gpio, false);
				System.out.println("Current val=" + mmap_gpio.gpioRead(gpio));
				SleepUtil.sleepSeconds(1);
			}

			boolean exit = false;
			if (exit) {
				System.exit(1);
			}

			if (true) {
				long start_ms = System.currentTimeMillis();

				for (int i = 0; i < iterations; i++) {
					mmap_gpio.gpioWrite(gpio, true);
					mmap_gpio.gpioWrite(gpio, false);
				}

				long duration_ms = System.currentTimeMillis() - start_ms;
				double frequency = iterations / (duration_ms / 1000.0);

				System.out.format("Duration for %,d iterations: %,.3f s, frequency: %,.0f Hz%n", iterations,
						((float) duration_ms) / 1000, frequency);
			}

			for (int i = 0; i < 5; i++) {
				System.out.println("on");
				mmap_gpio.gpioWrite(gpio, true);
				SleepUtil.sleepSeconds(1);
				System.out.println("off");
				mmap_gpio.gpioWrite(gpio, false);
				SleepUtil.sleepSeconds(1);
			}
		}
	}

	public static void test() {
		final ThreadLocalRandom rand = ThreadLocalRandom.current();
		IntBuffer buffer = ByteBuffer.allocateDirect(500).asIntBuffer();
		for (int i = 0; i < buffer.capacity(); i++) {
			buffer.put(rand.nextInt());
		}
		buffer.flip();
		Hex.dumpIntBuffer(buffer, 0, 2);
	}
}
