package com.diozero.internal.board.tinkerboard;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     TinkerBoardMmapGpio.java  
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
import com.diozero.internal.provider.MmapGpioInterface;
import com.diozero.util.LibraryLoader;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/*-
 * https://github.com/torvalds/linux/blob/master/arch/arm/boot/dts/rk3288.dtsi
 * https://chromium.googlesource.com/chromiumos/third_party/coreboot/+/chromeos-2013.04/src/soc/rockchip/rk3288/gpio.c
 * Note wiringTB was updated to use in-line asm for digital writes:
 * https://github.com/TinkerBoard/gpio_lib_c/blob/sbc/tinkerboard/c/wiringPi/wiringTB.c#L735
 */
@SuppressWarnings("resource")
public class TinkerBoardMmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/gpiomem";
	private static final int PMU_BASE = 0xff730000;
	private static final int GPIO_BASE = 0xff750000;
	private static final int GRF_BASE = 0xff770000;
	private static final int GPIO_LENGTH = 0x00010000;
	private static final int GPIO_CHANNEL = 0x00020000;

	private static final int PAGE_SIZE = 4 * 0x400;

	private static final int UNKNOWN = -1;

	private static final int PMU_GPIO0C_IOMUX_INT_OFFSET = 0x008c / 4;
	private static final int GRF_GPIO5B_IOMUX_INT_OFFSET = 0x0050 / 4;
	private static final int GRF_GPIO5C_IOMUX_INT_OFFSET = 0x0054 / 4;
	private static final int GRF_GPIO6A_IOMUX_INT_OFFSET = 0x005c / 4;
	private static final int GRF_GPIO6B_IOMUX_INT_OFFSET = 0x0060 / 4;
	private static final int GRF_GPIO6C_IOMUX_INT_OFFSET = 0x0064 / 4;
	private static final int GRF_GPIO7A_IOMUX_INT_OFFSET = 0x006c / 4;
	private static final int GRF_GPIO7B_IOMUX_INT_OFFSET = 0x0070 / 4;
	private static final int GRF_GPIO7CL_IOMUX_INT_OFFSET = 0x0074 / 4;
	private static final int GRF_GPIO7CH_IOMUX_INT_OFFSET = 0x0078 / 4;
	private static final int GRF_GPIO8A_IOMUX_INT_OFFSET = 0x0080 / 4;
	private static final int GRF_GPIO8B_IOMUX_INT_OFFSET = 0x0084 / 4;

	private static final int PMU_GPIO0C_P_INT_OFFSET = 0x006c / 4;
	private static final int GRF_GPIO5B_P_INT_OFFSET = 0x0184 / 4;
	private static final int GRF_GPIO5C_P_INT_OFFSET = 0x0188 / 4;
	private static final int GRF_GPIO6A_P_INT_OFFSET = 0x0190 / 4;
	// private static final int GRF_GPIO6B_P_INT_OFFSET = 0x0194 / 4;
	// private static final int GRF_GPIO6C_P_INT_OFFSET = 0x0198 / 4;
	private static final int GRF_GPIO7A_P_INT_OFFSET = 0x01a0 / 4;
	private static final int GRF_GPIO7B_P_INT_OFFSET = 0x01a4 / 4;
	private static final int GRF_GPIO7C_P_INT_OFFSET = 0x01a8 / 4;
	private static final int GRF_GPIO8A_P_INT_OFFSET = 0x01b0 / 4;
	private static final int GRF_GPIO8B_P_INT_OFFSET = 0x01b4 / 4;

	private static final int GPIO_SWPORTA_DR_INT_OFFSET = 0x0000 / 4;
	private static final int GPIO_SWPORTA_DDR_INT_OFFSET = 0x0004 / 4;
	private static final int GPIO_EXT_PORTA_INT_OFFSET = 0x0050 / 4;

	private static final int MUX_FUNC_GPIO = 0;
	// Only for GPIO7_C6 and GPIO7_C7
	private static final int MUX_FUNC_PWM = 3;

	private MmapIntBuffer[] gpioBanks;
	private MmapIntBuffer pmuMmapIntBuffer;
	private MmapIntBuffer grfMmapIntBuffer;

	@Override
	public synchronized void initialise() {
		if (gpioBanks == null) {
			gpioBanks = new MmapIntBuffer[9];
			for (int i = 0; i < gpioBanks.length; i++) {
				gpioBanks[i] = new MmapIntBuffer(MEM_DEVICE, GPIO_BASE + i * GPIO_LENGTH + (i > 0 ? GPIO_CHANNEL : 0),
						PAGE_SIZE, ByteOrder.LITTLE_ENDIAN);
			}
			pmuMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, PMU_BASE, PAGE_SIZE, ByteOrder.LITTLE_ENDIAN);
			grfMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, GRF_BASE, PAGE_SIZE, ByteOrder.LITTLE_ENDIAN);
		}
	}

	@Override
	public synchronized void close() {
		if (gpioBanks != null) {
			for (int i = 0; i < gpioBanks.length; i++) {
				gpioBanks[i].close();
			}
			gpioBanks = null;
		}
		if (pmuMmapIntBuffer != null) {
			pmuMmapIntBuffer.close();
			pmuMmapIntBuffer = null;
		}
		if (grfMmapIntBuffer != null) {
			grfMmapIntBuffer.close();
			grfMmapIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		// https://github.com/TinkerBoard/gpio_lib_c/blob/sbc/tinkerboard/c/wiringPi/wiringTB.c#L370
		MmapIntBuffer mux_int_buffer = (gpio < 24) ? pmuMmapIntBuffer : grfMmapIntBuffer;

		int iomux_offset = getIoMuxOffsetForGpio(gpio);
		if (iomux_offset == UNKNOWN) {
			Logger.warn("Unknown IOMUX offset for GPIO #{}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}

		int mux_mask = getMuxMask(gpio);
		if (mux_mask == UNKNOWN) {
			Logger.warn("Unknown mux mode for GPIO #{}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}

		int config_val = mux_int_buffer.get(iomux_offset);

		int mux_func;
		if (gpio == 238 || gpio == 239) {
			// The two PWM pins
			mux_func = (config_val >> (((gpio - 4) % 8) * 4)) & mux_mask;
		} else {
			mux_func = (config_val >> ((gpio % 8) * 2)) & mux_mask;
		}

		DeviceMode mode;
		if (mux_func == MUX_FUNC_GPIO) {
			// gpioToBank -
			// https://github.com/TinkerBoard/gpio_lib_c/blob/sbc/tinkerboard/c/wiringPi/wiringTB.c#L164
			// gpioToBankPin -
			// https://github.com/TinkerBoard/gpio_lib_c/blob/sbc/tinkerboard/c/wiringPi/wiringTB.c#L172
			int bank;
			int shift;
			if (gpio < 24) {
				// bank = gpio / 32;
				bank = 0;
				// shift = gpio % 32;
				shift = gpio;
			} else {
				// ((gpio - 24) / 32) + 1
				bank = (gpio + 8) / 32;
				// (gpio - 24) % 32
				shift = (gpio + 8) % 32;
			}
			if ((gpioBanks[bank].get(GPIO_SWPORTA_DDR_INT_OFFSET) & 1 << shift) != 0) {
				mode = DeviceMode.DIGITAL_OUTPUT;
			} else {
				mode = DeviceMode.DIGITAL_INPUT;
			}
		} else if ((gpio == 238 || gpio == 239) && mux_func == MUX_FUNC_PWM) {
			// PWM can be selected on 238 / 239
			mode = DeviceMode.PWM_OUTPUT;
		} else {
			mode = DeviceMode.UNKNOWN;
		}

		return mode;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		MmapIntBuffer mux_int_buffer = (gpio < 24) ? pmuMmapIntBuffer : grfMmapIntBuffer;

		int iomux_offset = getIoMuxOffsetForGpio(gpio);
		if (iomux_offset == UNKNOWN) {
			Logger.warn("Unknown IOMUX offset for GPIO #{}", Integer.valueOf(gpio));
			return;
		}

		// Get the current mode register value
		int config_val = mux_int_buffer.get(iomux_offset);

		// Configure mode
		switch (mode) {
		case DIGITAL_INPUT:
		case DIGITAL_OUTPUT:
			// 1. Configure as GPIO
			if (gpio == 233 || gpio == 234) {
				// *(grf+GRF_GPIO7CL_IOMUX/4) = (*(grf+GRF_GPIO7CL_IOMUX/4) |
				// (0x0f<<(16+(pin%8)*4))) & (~(0x0f<<((pin%8)*4)));
				config_val = (config_val | (0x0f << (16 + (gpio % 8) * 4))) & (~(0x0f << ((gpio % 8) * 4)));
			} else if (gpio == 238 || gpio == 239) {
				config_val = (config_val | (0x0f << (16 + (gpio % 8 - 4) * 4))) & (~(0x0f << ((gpio % 8 - 4) * 4)));
			} else {
				config_val = (config_val | (0x03 << ((gpio % 8) * 2 + 16))) & (~(0x03 << ((gpio % 8) * 2)));
			}

			// Update the mode register
			mux_int_buffer.put(iomux_offset, config_val);

			// 2. Set the GPIO direction
			int bank;
			int shift;
			if (gpio < 24) {
				bank = 0;
				shift = gpio;
			} else {
				bank = (gpio + 8) / 32;
				shift = (gpio + 8) % 32;
			}

			// Get the current GPIO direction register
			int gpio_val = gpioBanks[bank].get(GPIO_SWPORTA_DDR_INT_OFFSET);

			if (mode == DeviceMode.DIGITAL_INPUT) {
				gpio_val &= ~(1 << shift);
			} else {
				gpio_val |= (1 << shift);
			}

			// Update the GPIO direction register
			gpioBanks[bank].put(GPIO_SWPORTA_DDR_INT_OFFSET, gpio_val);
			break;
		case PWM_OUTPUT:
			Logger.warn("Mode {} not yet implemented for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		default:
			Logger.warn("Invalid mode ({}) for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		}
		
		// Seem to need a small delay...
		SleepUtil.sleepMillis(1);
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		Logger.debug("setPullUpDown({}, {})", Integer.valueOf(gpio), pud);
		int bit0, bit1;
		switch (pud) {
		case PULL_UP:
			bit0 = 1;
			bit1 = 0;
			break;
		case PULL_DOWN:
			bit0 = 0;
			bit1 = 1;
			break;
		case NONE:
		default:
			bit0 = 0;
			bit1 = 0;
			break;
		}
		MmapIntBuffer int_buffer = (gpio < 24) ? pmuMmapIntBuffer : grfMmapIntBuffer;
		int pud_offset = getPudOffsetForGpio(gpio);
		int_buffer.put(pud_offset,
				(int_buffer.get(pud_offset) | (0x03 << ((gpio % 8) * 2 + 16))) & (~(0x03 << ((gpio % 8) * 2)))
						| (bit1 << ((gpio % 8) * 2 + 1)) | (bit0 << ((gpio % 8) * 2)));
		// *(grf+pud) = (*(grf+pud) | (0x03<<((pin%8)*2+16))) & (~(0x03<<((pin%8)*2))) |
		// (bit1<<((pin%8)*2+1)) | (bit0<<((pin%8)*2));
	}

	@Override
	public boolean gpioRead(int gpio) {
		int bank;
		int shift;
		if (gpio < 24) {
			bank = gpio / 32;
			shift = gpio % 32;
		} else {
			bank = (gpio + 8) / 32;
			shift = (gpio + 8) % 32;
		}
		return (gpioBanks[bank].get(GPIO_EXT_PORTA_INT_OFFSET) & (1 << shift)) != 0;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		int bank;
		int shift;
		if (gpio < 24) {
			bank = gpio / 32;
			shift = gpio % 32;
		} else {
			bank = (gpio + 8) / 32;
			shift = (gpio + 8) % 32;
		}
		int reg_val = gpioBanks[bank].get(GPIO_SWPORTA_DR_INT_OFFSET);
		if (value) {
			reg_val |= (1 << shift);
		} else {
			reg_val &= ~(1 << shift);
		}
		gpioBanks[bank].put(GPIO_SWPORTA_DR_INT_OFFSET, reg_val);
	}

	private static int getIoMuxOffsetForGpio(int gpio) {
		switch (gpio) {
		// GPIO0_C1
		case 17:
			return PMU_GPIO0C_IOMUX_INT_OFFSET;
		// GPIO5B
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
			return GRF_GPIO5B_IOMUX_INT_OFFSET;
		// GPIO5C
		case 168:
		case 169:
		case 170:
		case 171:
			return GRF_GPIO5C_IOMUX_INT_OFFSET;
		// GPIO6A
		case 184:
		case 185:
		case 187:
		case 188:
			return GRF_GPIO6A_IOMUX_INT_OFFSET;
		// GPIO7A
		case 223:
			return GRF_GPIO7A_IOMUX_INT_OFFSET;
		// GPIO7B
		case 224:
		case 225:
		case 226:
			return GRF_GPIO7B_IOMUX_INT_OFFSET;
		case 233:
		case 234:
			return GRF_GPIO7CL_IOMUX_INT_OFFSET;
		case 238:
		case 239:
			return GRF_GPIO7CH_IOMUX_INT_OFFSET;
		// GPIO8A
		case 251:
		case 252:
		case 253:
		case 254:
		case 255:
			return GRF_GPIO8A_IOMUX_INT_OFFSET;
		// GPIO8B
		case 256:
		case 257:
			return GRF_GPIO8B_IOMUX_INT_OFFSET;
		default:
			return UNKNOWN;
		}
	}

	private static int getMuxMask(int gpio) {
		switch (gpio) {
		// GPIO0_C1
		case 17:
			return 0x00000003;
		// GPIO5B
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
			return 0x00000003;
		// GPIO5C
		case 168:
			return 0x00000003;
		case 169:
		case 170:
		case 171:
			return 0x00000001;
		// GPIO6A
		case 184:
		case 185:
		case 187:
		case 188:
			return 0x00000001;
		// GPIO7A
		case 223:
			return 0x00000003;
		// GPIO7B
		case 224:
		case 225:
		case 226:
			return 0x00000003;
		case 233:
		case 234:
			return 0x00000001;
		case 238:
			return 0x00000003;
		case 239:
			return 0x00000007;
		// GPIO8A
		case 251:
		case 252:
		case 253:
		case 254:
		case 255:
			return 0x00000003;
		// GPIO8B
		case 256:
		case 257:
			return 0x00000003;
		default:
			return UNKNOWN;
		}
	}

	private static int getPudOffsetForGpio(int gpio) {
		switch (gpio) {
		// GPIO0
		case 17:
			return PMU_GPIO0C_P_INT_OFFSET;
		// GPIO5B
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
			return GRF_GPIO5B_P_INT_OFFSET;
		// GPIO5C
		case 168:
		case 169:
		case 170:
		case 171:
			return GRF_GPIO5C_P_INT_OFFSET;
		// GPIO6A
		case 184:
		case 185:
		case 187:
		case 188:
			return GRF_GPIO6A_P_INT_OFFSET;
		// GPIO7A
		case 223:
			return GRF_GPIO7A_P_INT_OFFSET;
		// GPIO7B
		case 224:
		case 225:
		case 226:
			return GRF_GPIO7B_P_INT_OFFSET;
		case 233:
		case 234:
		case 238:
		case 239:
			return GRF_GPIO7C_P_INT_OFFSET;
		// GPIO8A
		case 251:
		case 252:
		case 253:
		case 254:
		case 255:
			return GRF_GPIO8A_P_INT_OFFSET;
		// GPIO8B
		case 256:
		case 257:
			return GRF_GPIO8B_P_INT_OFFSET;
		default:
			return UNKNOWN;
		}
	}

	public static void main(String[] args) {
		LibraryLoader.loadSystemUtils();
		
		int gpio;
		if (args.length > 0) {
			gpio = Integer.parseInt(args[0]);
		} else {
			gpio = 171;
		}
		
		try (TinkerBoardMmapGpio mmap_gpio = new TinkerBoardMmapGpio()) {
			mmap_gpio.initialise();

			DeviceMode mode = mmap_gpio.getMode(gpio);
			Logger.debug("Mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio), mode,
					Boolean.valueOf(mmap_gpio.gpioRead(gpio)));

			for (int i = 0; i < 5; i++) {
				// Toggle input/output mode
				mode = (mode == DeviceMode.DIGITAL_INPUT) ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT;

				Logger.debug("Setting mode for GPIO #{} to {}", Integer.valueOf(gpio), mode);
				mmap_gpio.setMode(gpio, mode);
				Logger.debug("Configured mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio),
						mmap_gpio.getMode(gpio), Boolean.valueOf(mmap_gpio.gpioRead(gpio)));

				SleepUtil.sleepMillis(500);
			}

			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			SleepUtil.sleepMillis(10);

			for (int i = 0; i < 5; i++) {
				Logger.debug("Setting GPIO #{} to On", Integer.valueOf(gpio));
				mmap_gpio.gpioWrite(gpio, true);
				Logger.debug("Mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio), mmap_gpio.getMode(gpio),
						Boolean.valueOf(mmap_gpio.gpioRead(gpio)));

				SleepUtil.sleepMillis(300);

				Logger.debug("Setting GPIO #{} to Off", Integer.valueOf(gpio));
				mmap_gpio.gpioWrite(gpio, false);
				Logger.debug("Mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio), mmap_gpio.getMode(gpio),
						Boolean.valueOf(mmap_gpio.gpioRead(gpio)));

				SleepUtil.sleepMillis(300);
			}
			
			int iterations = 1_000_000;
			for (int j = 0; j < 5; j++) {
				long start_nano = System.nanoTime();
				for (int i = 0; i < iterations; i++) {
					mmap_gpio.gpioWrite(gpio, true);
					mmap_gpio.gpioWrite(gpio, false);
				}
				long duration_ns = System.nanoTime() - start_nano;

				Logger.info("Duration for {} iterations: {}s", Integer.valueOf(iterations),
						String.format("%.4f", Float.valueOf(((float) duration_ns) / 1000 / 1000 / 1000)));
			}
		}
	}
}
