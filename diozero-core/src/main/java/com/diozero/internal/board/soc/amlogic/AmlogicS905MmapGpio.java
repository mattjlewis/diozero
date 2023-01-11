package com.diozero.internal.board.soc.amlogic;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OdroidC2MmapGpio.java
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
import java.util.concurrent.ThreadLocalRandom;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InvalidModeException;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.Hex;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/**
 * See <a href=
 * "https://github.com/hardkernel/wiringPi/blob/master/wiringPi/wiringPi.c">Odroid
 * wiringPi</a> fork. Odroid C2 / S905 Datasheet:
 * https://dn.odroid.com/S905/DataSheet/S905_Public_Datasheet_V1.1.4.pdf
 */
public class AmlogicS905MmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";

	private static final int J2_GPIO_BASE_ADDRESS = 0xC8834000;
	private static final int J7_GPIO_BASE_ADDRESS = 0xC8100000;
	private static final int BLOCK_SIZE = 4 * 1024;

	// XXX Armbian Kernel 5.x has the sysfs GPIO numbers starting at an offset of
	// 242 (0xF2) compared to Hardkernel 3.14
	// > cat /sys/class/gpio/gpiochip378/base
	// 378
	private static final int ARMBIAN_J2_GPIO_OFFSET_HACK = 242;
	private static final int J2_GPIO_START = 136 + ARMBIAN_J2_GPIO_OFFSET_HACK;
	// > cat /sys/class/gpio/gpiochip497/base
	// 497
	private static final int ARMBIAN_J7_GPIO_OFFSET_HACK = 375;
	private static final int J7_GPIO_START = 122 + ARMBIAN_J7_GPIO_OFFSET_HACK;

	private static final int J2_GPIODV_GPIO_START = J2_GPIO_START + 45;
	private static final int J2_GPIODV_PIN_END = J2_GPIO_START + 74;

	private static final int J2_GPIOY_PIN_START = J2_GPIO_START + 75;
	private static final int J2_GPIOY_PIN_END = J2_GPIO_START + 91;

	private static final int J2_GPIOX_PIN_START = J2_GPIO_START + 92;
	private static final int J2_GPIOX_PIN_END = J2_GPIO_START + 114;

	private static final int J7_GPIOA_PIN_START = J7_GPIO_START;
	private static final int J7_GPIOA_PIN_END = J7_GPIO_START + 14;

	private static final int J2_GPIODV_OEN_REG_OFFSET = 0x10C;
	private static final int J2_GPIODV_OUT_REG_OFFSET = 0x10D;
	private static final int J2_GPIODV_INP_REG_OFFSET = 0x10E;
	private static final int J2_GPIODV_PUEN_REG_OFFSET = 0x148;
	private static final int J2_GPIODV_PUPD_REG_OFFSET = 0x13A;

	private static final int J2_GPIOY_OEN_REG_OFFSET = 0x10F;
	private static final int J2_GPIOY_OUT_REG_OFFSET = 0x110;
	private static final int J2_GPIOY_INP_REG_OFFSET = 0x111;
	private static final int J2_GPIOY_PUEN_REG_OFFSET = 0x149;
	private static final int J2_GPIOY_PUPD_REG_OFFSET = 0x13B;

	private static final int J2_GPIOX_OEN_REG_OFFSET = 0x118;
	private static final int J2_GPIOX_OUT_REG_OFFSET = 0x119;
	private static final int J2_GPIOX_INP_REG_OFFSET = 0x11A;
	private static final int J2_GPIOX_PUEN_REG_OFFSET = 0x14C;
	private static final int J2_GPIOX_PUPD_REG_OFFSET = 0x13E;

	private static final int J7_GPIOA_OEN_REG_OFFSET = 0x09;
	private static final int J7_GPIOA_OUT_REG_OFFSET = 0x09;
	private static final int J7_GPIOA_INP_REG_OFFSET = 0x0A;
	private static final int J7_GPIOA_PUEN_REG_OFFSET = 0x0b;
	private static final int J7_GPIOA_PUPD_REG_OFFSET = 0x0b;

	/*
	 * private static final int C2_MUX_REG_0_OFFSET = 0x2C; private static final int
	 * C2_MUX_REG_1_OFFSET = 0x2D; private static final int C2_MUX_REG_2_OFFSET =
	 * 0x2E; private static final int C2_MUX_REG_3_OFFSET = 0x2F; private static
	 * final int C2_MUX_REG_4_OFFSET = 0x30; private static final int
	 * C2_MUX_REG_5_OFFSET = 0x31; private static final int C2_MUX_REG_7_OFFSET =
	 * 0x33; private static final int C2_MUX_REG_8_OFFSET = 0x34;
	 */

	// private static final int[] C2_GP_TO_SHIFT_REG = new int[] { //
	// 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, //
	// 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
	// 22 };

	private boolean initialised;
	private MmapIntBuffer j2MmapIntBuffer;
	private MmapIntBuffer j7MmapIntBuffer;
	// private MmapByteBuffer mmap;
	// private volatile IntBuffer gpioIntBuffer;

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			// mmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, C2_GPIO_BASE,
			// BLOCK_SIZE);
			// gpioIntBuffer =
			// mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			j2MmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, J2_GPIO_BASE_ADDRESS, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			j7MmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, J7_GPIO_BASE_ADDRESS, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);

			initialised = true;
		}
	}

	@Override
	public synchronized void close() {
		if (initialised) {
			// MmapBufferNative.closeMmapBuffer(mmap.getFd(), mmap.getAddress(),
			// mmap.getLength());
			j2MmapIntBuffer.close();
			j7MmapIntBuffer.close();
		}
	}

	/*
	 * Offset to the GPIO Output Enable register
	 */
	private static final int gpioToOutputEnableRegOffset(int gpio) {
		if (gpio >= J2_GPIODV_GPIO_START && gpio <= J2_GPIODV_PIN_END) {
			return J2_GPIODV_OEN_REG_OFFSET;
		}
		if (gpio >= J2_GPIOY_PIN_START && gpio <= J2_GPIOY_PIN_END) {
			return J2_GPIOY_OEN_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_OEN_REG_OFFSET;
		}
		if (gpio >= J7_GPIOA_PIN_START && gpio <= J7_GPIOA_PIN_END) {
			return J7_GPIOA_OEN_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Output register
	 */
	private static int gpioToOutputRegOffset(int gpio) {
		if (gpio >= J2_GPIODV_GPIO_START && gpio <= J2_GPIODV_PIN_END) {
			return J2_GPIODV_OUT_REG_OFFSET;
		}
		if (gpio >= J2_GPIOY_PIN_START && gpio <= J2_GPIOY_PIN_END) {
			return J2_GPIOY_OUT_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_OUT_REG_OFFSET;
		}
		if (gpio >= J7_GPIOA_PIN_START && gpio <= J7_GPIOA_PIN_END) {
			return J7_GPIOA_OUT_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Input register
	 */
	private static final int gpioToInputRegOffset(int gpio) {
		if (gpio >= J2_GPIODV_GPIO_START && gpio <= J2_GPIODV_PIN_END) {
			return J2_GPIODV_INP_REG_OFFSET;
		}
		if (gpio >= J2_GPIOY_PIN_START && gpio <= J2_GPIOY_PIN_END) {
			return J2_GPIOY_INP_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_INP_REG_OFFSET;
		}
		if (gpio >= J7_GPIOA_PIN_START && gpio <= J7_GPIOA_PIN_END) {
			return J7_GPIOA_INP_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Pull up/down enable register
	 */
	private static int gpioToPullUpDownEnableRegOffset(int gpio) {
		if (gpio >= J2_GPIODV_GPIO_START && gpio <= J2_GPIODV_PIN_END) {
			return J2_GPIODV_PUEN_REG_OFFSET;
		}
		if (gpio >= J2_GPIOY_PIN_START && gpio <= J2_GPIOY_PIN_END) {
			return J2_GPIOY_PUEN_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_PUEN_REG_OFFSET;
		}
		if (gpio >= J7_GPIOA_PIN_START && gpio <= J7_GPIOA_PIN_END) {
			return J7_GPIOA_PUEN_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Pull up/down register
	 */
	private static int gpioToPullUpDownRegOffset(int gpio) {
		if (gpio >= J2_GPIODV_GPIO_START && gpio <= J2_GPIODV_PIN_END) {
			return J2_GPIODV_PUPD_REG_OFFSET;
		}
		if (gpio >= J2_GPIOY_PIN_START && gpio <= J2_GPIOY_PIN_END) {
			return J2_GPIOY_PUPD_REG_OFFSET;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return J2_GPIOX_PUPD_REG_OFFSET;
		}
		if (gpio >= J7_GPIOA_PIN_START && gpio <= J7_GPIOA_PIN_END) {
			return J7_GPIOA_PUPD_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO bit
	 */
	private static int gpioToRegShiftBit(int gpio) {
		if (gpio >= J2_GPIODV_GPIO_START && gpio <= J2_GPIODV_PIN_END) {
			return gpio - J2_GPIODV_GPIO_START;
		}
		if (gpio >= J2_GPIOY_PIN_START && gpio <= J2_GPIOY_PIN_END) {
			return gpio - J2_GPIOY_PIN_START;
		}
		if (gpio >= J2_GPIOX_PIN_START && gpio <= J2_GPIOX_PIN_END) {
			return gpio - J2_GPIOX_PIN_START;
		}
		if (gpio >= J7_GPIOA_PIN_START && gpio <= J7_GPIOA_PIN_END) {
			return gpio - J7_GPIOA_PIN_START;
		}
		return -1;
	}

	private MmapIntBuffer getMmapIntBuffer(int gpio) {
		if (gpio < J7_GPIOA_PIN_START) {
			return j2MmapIntBuffer;
		}

		return j7MmapIntBuffer;
	}

	@Override
	public DeviceMode getMode(int gpio) {
		int fsel = gpioToOutputEnableRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);
		// int fsel = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET :
		// C2_GPIOX_FSEL_REG_OFFSET;
		// int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];

		if (fsel == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}

		// return (gpioReg.get(fsel) & (1 << shift)) == 0 ? DeviceMode.DIGITAL_OUTPUT :
		// DeviceMode.DIGITAL_INPUT;
		// switch (gpioIntBuffer.get(fsel) & (1 << shift)) {
		MmapIntBuffer mmap_int_buffer = getMmapIntBuffer(gpio);
		if (mmap_int_buffer.get(fsel, 1 << shift) == 0) {
			return DeviceMode.DIGITAL_OUTPUT;
		}

		return DeviceMode.DIGITAL_INPUT;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			// gpioIntBuffer.put(fsel, gpioIntBuffer.get(fsel) | (1 << shift));
			// *(gpio + fsel) = (*(gpio + fsel) | (1 << shift));
			// mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) | (1 << shift));
			setModeUnchecked(gpio, 1);
			// _pullUpDnControl(origPin, PUD_OFF);
			// _pullUpDnControl(origPin, PUD_ON);
			break;
		case DIGITAL_OUTPUT:
			// gpioIntBuffer.put(fsel, gpioIntBuffer.get(fsel) & ~(1 << shift));
			// *(gpio + fsel) = (*(gpio + fsel) & ~(1 << shift));
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
		// int fsel = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET :
		// C2_GPIOX_FSEL_REG_OFFSET;
		// int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];

		if (fsel == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
		}

		MmapIntBuffer mmap_int_buffer = getMmapIntBuffer(gpio);
		// Mode can only be 0 or 1
		if (mode == 0) {
			mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) & ~(1 << shift));
		} else {
			mmap_int_buffer.put(fsel, mmap_int_buffer.get(fsel) | (1 << shift));
		}
	}

	public int getPullUpDown(int gpio) {
		int puen = gpioToPullUpDownEnableRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);

		if (puen == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return -1;
		}

		MmapIntBuffer mmap_int_buffer = getMmapIntBuffer(gpio);
		if ((mmap_int_buffer.get(puen) & (1 << shift)) != 0) {
			int pupd = gpioToPullUpDownRegOffset(gpio);
			if (gpio >= J7_GPIOA_PIN_START) {
				shift += 16;
			}
			return (mmap_int_buffer.get(pupd) & (1 << shift)) == 0 ? 2 : 1;
		}
		return 0;
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		int shift = gpioToRegShiftBit(gpio);
		int puen = gpioToPullUpDownEnableRegOffset(gpio);
		// int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		// int puen = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_PUEN_REG_OFFSET :
		// C2_GPIOX_PUEN_REG_OFFSET;

		if (puen == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return;
		}

		MmapIntBuffer mmap_int_buffer = getMmapIntBuffer(gpio);
		if (pud == GpioPullUpDown.NONE) {
			// Disable Pull/Pull-down resister
			// gpioIntBuffer.put(puen, gpioIntBuffer.get(puen) & ~(1 << shift));
			// *(gpio + puen) = (*(gpio + puen) & ~(1 << shift));
			mmap_int_buffer.put(puen, mmap_int_buffer.get(puen) & ~(1 << shift));
		} else {
			// Enable Pull/Pull-down resister
			// gpioIntBuffer.put(puen, gpioIntBuffer.get(puen) | (1 << shift));
			// *(gpio + puen) = (*(gpio + puen) | (1 << shift));
			mmap_int_buffer.put(puen, mmap_int_buffer.get(puen) | (1 << shift));

			int pupd = gpioToPullUpDownRegOffset(gpio);
			if (gpio >= J7_GPIOA_PIN_START) {
				shift += 16;
			}
			// int pupd = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_PUPD_REG_OFFSET :
			// C2_GPIOX_PUPD_REG_OFFSET;
			if (pud == GpioPullUpDown.PULL_UP) {
				// gpioIntBuffer.put(pupd, gpioIntBuffer.get(pupd) | (1 << shift));
				// *(gpio + pupd) = (*(gpio + pupd) | (1 << shift));
				mmap_int_buffer.put(pupd, mmap_int_buffer.get(pupd) | (1 << shift));
			} else {
				// gpioIntBuffer.put(pupd, gpioIntBuffer.get(pupd) & ~(1 << shift));
				// *(gpio + pupd) = (*(gpio + pupd) & ~(1 << shift));
				mmap_int_buffer.put(pupd, mmap_int_buffer.get(pupd) & ~(1 << shift));
			}
		}
	}

	@Override
	public boolean gpioRead(int gpio) {
		int reg = gpioToInputRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);
		// int reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_INP_REG_OFFSET :
		// C2_GPIOX_INP_REG_OFFSET;
		// int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];

		if (reg == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return false;
		}

		MmapIntBuffer mmap_int_buffer = getMmapIntBuffer(gpio);
		// return (gpioIntBuffer.get(gp_lev_reg) & (1 << shift)) != 0;
		/*-
		if ((*(gpio + reg) & (1 << shift)) != 0)
			return HIGH;
		else
			return LOW;
		 */
		return (mmap_int_buffer.get(reg) & (1 << shift)) != 0;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		// Note no boundary checks to maximise performance

		int reg = gpioToOutputRegOffset(gpio);
		int shift = gpioToRegShiftBit(gpio);
		if (gpio >= J7_GPIOA_PIN_START) {
			shift += 16;
		}
		// int reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_OUTP_REG_OFFSET :
		// C2_GPIOX_OUTP_REG_OFFSET;
		// int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];

		MmapIntBuffer mmap_int_buffer = getMmapIntBuffer(gpio);
		if (value) {
			// gpioIntBuffer.put(reg, gpioIntBuffer.get(reg) | (1 << shift));
			// *(gpio + reg) |= (1 << shift);
			mmap_int_buffer.put(reg, mmap_int_buffer.get(reg) | (1 << shift));
		} else {
			// gpioIntBuffer.put(reg, gpioIntBuffer.get(reg) & ~(1 << shift));
			// *(gpio + reg) &= ~(1 << shift);
			mmap_int_buffer.put(reg, mmap_int_buffer.get(reg) & ~(1 << shift));
		}
	}

	public static void main(String[] args) {
		System.out.println(ByteOrder.nativeOrder());
		if (args.length != 2) {
			System.out.println("Usage: " + AmlogicS905MmapGpio.class.getName() + " <gpio> <iterations>");
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);
		int iterations = Integer.parseInt(args[1]);

		int gp_set_reg = gpio < J2_GPIOX_PIN_START ? J2_GPIOY_OUT_REG_OFFSET : J2_GPIOX_OUT_REG_OFFSET;
		int gp_set_reg2 = gpioToOutputRegOffset(gpio);
		System.out.println("gp_set_reg=" + gp_set_reg + ", gp_set_reg2=" + gp_set_reg2);
		// int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		// int shift2 = gpioToShiftReg(gpio);
		// System.out.println("shift=" + shift + ", shift2=" + shift2);
		int gp_lev_reg = gpio < J2_GPIOX_PIN_START ? J2_GPIOY_INP_REG_OFFSET : J2_GPIOX_INP_REG_OFFSET;
		int gp_lev_reg2 = gpioToInputRegOffset(gpio);
		System.out.println("gp_lev_reg=" + gp_lev_reg + ", gp_lev_reg2=" + gp_lev_reg2);
		int gp_fsel_reg = gpio < J2_GPIOX_PIN_START ? J2_GPIOY_OEN_REG_OFFSET : J2_GPIOX_OEN_REG_OFFSET;
		int gp_fsel_reg2 = gpioToOutputEnableRegOffset(gpio);
		System.out.println("gp_fsel_reg=" + gp_fsel_reg + ", gp_fsel_reg2=" + gp_fsel_reg2);

		System.out.format("gpioToGPSETReg(%d)=0x%04x%n", Integer.valueOf(214),
				Integer.valueOf(gpioToOutputRegOffset(214)));
		System.out.format("gpioToGPSETReg(%d)=0x%04x%n", Integer.valueOf(219),
				Integer.valueOf(gpioToOutputRegOffset(219)));
		System.out.format("gpioToGPFSELReg(%d)=0x%04x%n", Integer.valueOf(214),
				Integer.valueOf(gpioToOutputEnableRegOffset(214)));
		System.out.format("gpioToGPFSELReg(%d)=0x%04x%n", Integer.valueOf(219),
				Integer.valueOf(gpioToOutputEnableRegOffset(219)));

		try (AmlogicS905MmapGpio mmap_gpio = new AmlogicS905MmapGpio()) {
			mmap_gpio.initialise();

			/*
			 * BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			 * try { while (true) { Hex.dumpIntBuffer(mmap_gpio.gpioIntBuffer,
			 * C2_GPIO_PIN_BASE, 200); String line = reader.readLine(); if (line == null ||
			 * line.equals("q")) { break; } } } catch (IOException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */

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

				System.out.format("Duration for %,d iterations: %,.3f s, frequency: %,.0f Hz%n",
						Integer.valueOf(iterations), Float.valueOf(((float) duration_ms) / 1000),
						Double.valueOf(frequency));
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
