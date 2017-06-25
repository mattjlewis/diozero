package com.diozero.internal.board.tinkerboard;

/*
 * #%L
 * Device I/O Zero - high performance memory map GPIO control
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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
import com.diozero.internal.provider.mmap.MmapGpioInterface;
import com.diozero.util.MmapBufferNative;
import com.diozero.util.MmapByteBuffer;

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
	
	private static final int PMU_GPIO0C_IOMUX_INT_OFFSET = 0x008c / 4;
	
	private static final int GRF_GPIO5B_IOMUX_INT_OFFSET = 0x0050 / 4;
	private static final int GRF_GPIO5C_IOMUX_INT_OFFSET = 0x0054 / 4;
	private static final int GRF_GPIO6A_IOMUX_INT_OFFSET = 0x005c / 4;
	private static final int GRF_GPIO7A_IOMUX_INT_OFFSET = 0x006c / 4;
	private static final int GRF_GPIO7B_IOMUX_INT_OFFSET = 0x0070 / 4;
	private static final int GRF_GPIO7CL_IOMUX_INT_OFFSET = 0x0074 / 4;
	private static final int GRF_GPIO7CH_IOMUX_INT_OFFSET = 0x0078 / 4;
	private static final int GRF_GPIO8A_IOMUX_INT_OFFSET = 0x0080 / 4;
	private static final int GRF_GPIO8B_IOMUX_INT_OFFSET = 0x0084 / 4;
	
	private static final int UNKNOWN_IOMUX_INT_OFFSET = -1;
	
	private static final int GPIO_SWPORTA_DDR_INT_OFFSET = 0x0004 / 4;
	private static final int GPIO_EXT_PORTA_INT_OFFSET = 0x0050 / 4;
	
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
		int iomux = getIoMuxForGpio(gpio);
		if (iomux == UNKNOWN_IOMUX_INT_OFFSET) {
			Logger.warn("Unknown IOMUX offset for GPIO {}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}
		int config_val = mux_int_buffer.get(iomux);
		int mux_func;
		if (gpio == 238 || gpio == 239) {
			mux_func = (config_val >> (((gpio-4)%8)*4)) & 0x00000007;
		} else {
			mux_func = (config_val >> ((gpio%8)*2)) & 0x00000007;
		}
		
		DeviceMode mode;
		if (mux_func == 0) {
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
		} else if (mux_func == 3 && (gpio == 238 || gpio == 239)) {
			// PWM can be selected on 238 / 239
			mode = DeviceMode.PWM_OUTPUT;
		} else {
			mode = DeviceMode.UNKNOWN;
		}
		
		return mode;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		// TODO Auto-generated method stub
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
		return (((gpioBanks[bank].gpioIntBuffer.get(GPIO_EXT_PORTA_INT_OFFSET)) & (1 << shift)) >> shift) != 0;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		// TODO Auto-generated method stub
	}
	
	private static int getIoMuxForGpio(int gpio) {
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
			return UNKNOWN_IOMUX_INT_OFFSET;
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
}
