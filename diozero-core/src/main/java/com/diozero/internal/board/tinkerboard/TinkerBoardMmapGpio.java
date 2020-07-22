package com.diozero.internal.board.tinkerboard;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     TinkerBoardMmapGpio.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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
import java.nio.IntBuffer;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.board.chip.ChipMmapGpio;
import com.diozero.internal.provider.MmapGpioInterface;
import com.diozero.util.LibraryLoader;
import com.diozero.util.MmapBufferNative;
import com.diozero.util.MmapByteBuffer;
import com.diozero.util.SleepUtil;

/*
 * https://github.com/torvalds/linux/blob/master/arch/arm/boot/dts/rk3288.dtsi
 * https://chromium.googlesource.com/chromiumos/third_party/coreboot/+/chromeos-2013.04/src/soc/rockchip/rk3288/gpio.c
 */
public class TinkerBoardMmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";
	private static final int PMU_BASE		= 0xff730000;
	private static final int GPIO_BASE		= 0xff750000;
	private static final int GRF_BASE		= 0xff770000;
	private static final int GPIO_LENGTH	= 0x00010000;
	private static final int GPIO_CHANNEL	= 0x00020000;
	
	private static final int PAGE_SIZE = 4 * 0x400;
	
	private static final int UNKNOWN = -1;
	
	private static final int PMU_GPIO0C_IOMUX_INT_OFFSET = 0x008c / 4;
	private static final int GRF_GPIO5B_IOMUX_INT_OFFSET = 0x0050 / 4;
	private static final int GRF_GPIO5C_IOMUX_INT_OFFSET = 0x0054 / 4;
	private static final int GRF_GPIO6A_IOMUX_INT_OFFSET = 0x005c / 4;
	//private static final int GRF_GPIO6B_IOMUX_INT_OFFSET = 0x0060 / 4;
	//private static final int GRF_GPIO6C_IOMUX_INT_OFFSET = 0x0064 / 4;
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
	//private static final int GRF_GPIO6B_P_INT_OFFSET = 0x0194 / 4;
	//private static final int GRF_GPIO6C_P_INT_OFFSET = 0x0198 / 4;
	private static final int GRF_GPIO7A_P_INT_OFFSET = 0x01a0 / 4;
	private static final int GRF_GPIO7B_P_INT_OFFSET = 0x01a4 / 4;
	private static final int GRF_GPIO7C_P_INT_OFFSET = 0x01a8 / 4;
	private static final int GRF_GPIO8A_P_INT_OFFSET = 0x01b0 / 4;
	private static final int GRF_GPIO8B_P_INT_OFFSET = 0x01b4 / 4;
	
	private static final int GPIO_SWPORTA_DR_INT_OFFSET = 0x0000 / 4;
	private static final int GPIO_SWPORTA_DDR_INT_OFFSET = 0x0004 / 4;
	private static final int GPIO_EXT_PORTA_INT_OFFSET = 0x0050 / 4;
	private static final int MUX_FUNC_GPIO = 0;
	private static final int MUX_FUNC_PWM = 3;
	
	private GpioBank[] gpioBanks;
	private MmapByteBuffer pmuMmap;
	private IntBuffer pmuIntBuffer;
	private MmapByteBuffer grfMmap;
	private IntBuffer grfIntBuffer;
	
	@Override
	public synchronized void initialise() {
		if (gpioBanks == null) {
			gpioBanks = new GpioBank[9];
			for (int i=0; i<gpioBanks.length; i++) {
				gpioBanks[i] = new GpioBank(GPIO_BASE + i*GPIO_LENGTH + (i>0 ? GPIO_CHANNEL : 0));
			}
			pmuMmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, PMU_BASE, PAGE_SIZE);
			pmuIntBuffer = pmuMmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			grfMmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, GRF_BASE, PAGE_SIZE);
			grfIntBuffer = grfMmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
		}
	}

	@Override
	public synchronized void close() {
		if (gpioBanks != null) {
			for (int i=0; i<gpioBanks.length; i++) {
				MmapBufferNative.closeMmapBuffer(gpioBanks[i].mmap.getFd(), gpioBanks[i].mmap.getAddress(),
						gpioBanks[i].mmap.getLength());
			}
			gpioBanks = null;
		}
		if (pmuMmap != null) {
			MmapBufferNative.closeMmapBuffer(pmuMmap.getFd(), pmuMmap.getAddress(),
					pmuMmap.getLength());
			pmuMmap = null;
			pmuIntBuffer = null;
		}
		if (grfMmap != null) {
			MmapBufferNative.closeMmapBuffer(grfMmap.getFd(), grfMmap.getAddress(),
					grfMmap.getLength());
			grfMmap = null;
			grfIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		IntBuffer mux_int_buffer = (gpio < 24) ? pmuIntBuffer : grfIntBuffer;
		int iomux_offset = getIoMuxOffsetForGpio(gpio);
		if (iomux_offset == UNKNOWN) {
			Logger.warn("Unknown IOMUX offset for GPIO #{}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}
		int config_val = mux_int_buffer.get(iomux_offset);
		int mux_func;
		int mux_mask = getMuxMask(gpio);
		if (gpio == 238 || gpio == 239) {
			mux_func = (config_val >> (((gpio-4)%8)*4)) & mux_mask;
		} else {
			mux_func = (config_val >> ((gpio%8)*2)) & mux_mask;
		}

		DeviceMode mode;
		if (mux_func == MUX_FUNC_GPIO) {
			int bank;
			int shift;
			if (gpio < 24) {
				bank = gpio / 32;
				shift = gpio % 32;
			} else {
				bank = (gpio + 8) / 32;
				shift = (gpio + 8) % 32;
			}
			if ((gpioBanks[bank].gpioIntBuffer.get(GPIO_SWPORTA_DDR_INT_OFFSET) & 1<<shift) != 0) {
				mode = DeviceMode.DIGITAL_OUTPUT;
			} else {
				mode = DeviceMode.DIGITAL_INPUT;
			}
		} else if (mux_func == MUX_FUNC_PWM && (gpio == 238 || gpio == 239)) {
			// PWM can be selected on 238 / 239
			mode = DeviceMode.PWM_OUTPUT;
		} else {
			mode = DeviceMode.UNKNOWN;
		}
		
		return mode;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		IntBuffer mux_int_buffer = (gpio < 24) ? pmuIntBuffer : grfIntBuffer;
		int iomux_offset = getIoMuxOffsetForGpio(gpio);
		if (iomux_offset == UNKNOWN) {
			Logger.warn("Unknown IOMUX offset for GPIO #{}", Integer.valueOf(gpio));
			return;
		}
		// Configure mode
		int config_val = mux_int_buffer.get(iomux_offset);
		switch (mode) {
		case DIGITAL_INPUT:
		case DIGITAL_OUTPUT:
			if (gpio == 238 || gpio == 239) {
				config_val = (config_val | (0x0f<<(16+(gpio%8-4)*4))) & (~(0x0f<<((gpio%8-4)*4)));
			} else {
				config_val = (config_val | (0x03<<((gpio%8)*2+16))) & (~(0x03<<((gpio%8)*2)));
			}
			mux_int_buffer.put(iomux_offset, config_val);
			
			// Set digital direction
			int bank;
			int shift;
			if (gpio < 24) {
				bank = gpio / 32;
				shift = gpio % 32;
			} else {
				bank = (gpio + 8) / 32;
				shift = (gpio + 8) % 32;
			}
			int gpio_val = gpioBanks[bank].gpioIntBuffer.get(GPIO_SWPORTA_DDR_INT_OFFSET);
			if (mode == DeviceMode.DIGITAL_INPUT) {
				gpio_val &= ~(1<<shift);
			} else {
				gpio_val |= (1<<shift);
			}
			gpioBanks[bank].gpioIntBuffer.put(GPIO_SWPORTA_DDR_INT_OFFSET, gpio_val);
			break;
		case PWM_OUTPUT:
			Logger.warn("Mode {} not yet implemented for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		default:
			Logger.warn("Invalid mode ({}) for GPIO #{}", mode, Integer.valueOf(gpio));
			return;
		}
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
		IntBuffer int_buffer = (gpio < 24) ? pmuIntBuffer : grfIntBuffer;
		int pud_offset = getPudOffsetForGpio(gpio);
		int_buffer.put(pud_offset,
				(int_buffer.get(pud_offset) | (0x03 << ((gpio % 8) * 2 + 16))) & (~(0x03 << ((gpio % 8) * 2)))
						| (bit1 << ((gpio % 8) * 2 + 1)) | (bit0 << ((gpio % 8) * 2)));
		//*(grf+pud) = (*(grf+pud) | (0x03<<((pin%8)*2+16))) & (~(0x03<<((pin%8)*2))) | (bit1<<((pin%8)*2+1)) | (bit0<<((pin%8)*2));
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
		return (gpioBanks[bank].gpioIntBuffer.get(GPIO_EXT_PORTA_INT_OFFSET) & (1 << shift)) != 0;
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
		int reg_val = gpioBanks[bank].gpioIntBuffer.get(GPIO_SWPORTA_DR_INT_OFFSET);
		if (value) {
			reg_val |= (1<<shift);
		} else {
			reg_val &= ~(1<<shift);
		}
		gpioBanks[bank].gpioIntBuffer.put(GPIO_SWPORTA_DR_INT_OFFSET, reg_val);
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
	
	private static class GpioBank {
		MmapByteBuffer mmap;
		IntBuffer gpioIntBuffer;
		
		public GpioBank(int offset) {
			mmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, offset, PAGE_SIZE);
			gpioIntBuffer = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
		}
	}
	
	public static void main(String[] args) {
		LibraryLoader.loadLibrary(ChipMmapGpio.class, "diozero-system-utils");
		try (TinkerBoardMmapGpio mmap_gpio = new TinkerBoardMmapGpio()) {
			mmap_gpio.initialise();
			
			int gpio = 184;
			
			for (int i=0; i<10; i++) {
				DeviceMode mode = mmap_gpio.getMode(gpio);
				Logger.debug("Mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio), mode,
						Boolean.valueOf(mmap_gpio.gpioRead(gpio)));
				SleepUtil.sleepSeconds(1);

				// Toggle input/output mode
				mmap_gpio.setMode(gpio,
						mode == DeviceMode.DIGITAL_INPUT ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);
			}
			
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			
			for (int i=0; i<10; i++) {
				Logger.debug("GPIO #{} On", Integer.valueOf(gpio));
				mmap_gpio.gpioWrite(gpio, true);
				Logger.debug("Mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio), mmap_gpio.getMode(gpio),
						Boolean.valueOf(mmap_gpio.gpioRead(gpio)));
				SleepUtil.sleepSeconds(1);
				
				Logger.debug("GPIO #{} Off", Integer.valueOf(gpio));
				mmap_gpio.gpioWrite(gpio, false);
				Logger.debug("Mode for GPIO #{}: {}, value: {}", Integer.valueOf(gpio), mmap_gpio.getMode(gpio),
						Boolean.valueOf(mmap_gpio.gpioRead(gpio)));
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
