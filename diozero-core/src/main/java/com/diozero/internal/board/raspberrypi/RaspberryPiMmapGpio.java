package com.diozero.internal.board.raspberrypi;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     RaspberryPiMmapGpio.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.MmapGpioInterface;
import com.diozero.util.MmapBufferNative;
import com.diozero.util.MmapByteBuffer;
import com.diozero.util.SleepUtil;

@SuppressWarnings("unused")
public class RaspberryPiMmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/gpiomem";
	//private static final int GPIOMEM_LEN = 0xB4;
	private static final int GPIOMEM_LEN = 4096;
	
	// From BCM2835 data-sheet, p.91
	private static final byte GPFSEL_OFFSET   = 0x00 >> 2;
	private static final byte GPSET_OFFSET    = 0x1c >> 2;
	private static final byte GPCLR_OFFSET    = 0x28 >> 2;
	private static final byte GPLEV_OFFSET    = 0x34 >> 2;
	private static final byte GPEDS_OFFSET    = 0x40 >> 2;
	private static final byte GPREN_OFFSET    = 0x4c >> 2;
	private static final byte GPFEN_OFFSET    = 0x58 >> 2;
	private static final byte GPHEN_OFFSET    = 0x64 >> 2;
	private static final byte GPLEN_OFFSET    = 0x70 >> 2;
	private static final byte GPAREN_OFFSET   = 0x7c >> 2;
	private static final byte GPAFEN_OFFSET   = 0x88 >> 2;
	private static final byte GPPUD_OFFSET    = 0x94 >> 2;
	private static final byte GPPUDCLK_OFFSET = 0x98 >> 2;

	// Offset to the GPIO Set registers for each GPIO pin
	private static final byte[] GPIO_TO_GPSET = {
			7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
			8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
	};
	// Offset to the GPIO Clear registers for each GPIO pin
	private static final byte[] GPIO_TO_GPCLR = {
			10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,
			11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11
	};
	// Offset to the GPIO Input level registers for each GPIO pin
	private static final byte[] GPIO_TO_GPLEV = {
			13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
			14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14
	};
	// GPIO Pin pull up/down register
	private static final byte GPPUD = 37;
	// Offset to the Pull Up Down Clock register
	private static byte[] GPIO_TO_PUDCLK = {
			38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,
			39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39
	};
	
	private static final int PI_PUD_OFF = 0;
	private static final int PI_PUD_DOWN = 1;
	private static final int PI_PUD_UP = 2;
	
	private boolean initialised;
	private MmapByteBuffer mmap;
	private IntBuffer gpioIntBuffer;
	
	@Override
	public synchronized void initialise() {
		if (! initialised) {
			mmap = MmapBufferNative.createMmapBuffer(GPIOMEM_DEVICE, 0, GPIOMEM_LEN);
			gpioIntBuffer = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			
			initialised = true;
		}
	}
	
	@Override
	public synchronized void close() {
		if (initialised) {
			MmapBufferNative.closeMmapBuffer(mmap.getFd(), mmap.getAddress(), mmap.getLength());
		}
	}
	
	/**
	 * Returns the function of a GPIO: 0=input, 1=output, 4=alt0
	 * @param gpio GPIO number
	 * @return GPIO mode (0 - INPUT, 1 - OUTPUT)
	 */
	@Override
	public DeviceMode getMode(int gpio) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;

		switch ((gpioIntBuffer.get(reg) >> shift) & 7) {
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
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;
		
		switch (mode) {
		case DIGITAL_INPUT:
			gpioIntBuffer.put(reg, gpioIntBuffer.get(reg) & ~(7 << shift));
			break;
		case DIGITAL_OUTPUT:
			gpioIntBuffer.put(reg, (gpioIntBuffer.get(reg) & ~(7 << shift)) | (1 << shift));
			break;
		default:
			throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}
	
	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		// pigpio:
		/*
		gpioReg.put(GPPUD, pud);
		// Sleep 20us
		SleepUtil.busySleep(20_000);
		gpioReg.put(GPPUDCLK0 + gpio >> 5, 1 << (gpio & 0x1F));
		SleepUtil.busySleep(20_000);
		gpioReg.put(GPPUD, 0);
		gpioReg.put(GPPUDCLK0 + gpio >> 5, 0);
		*/
		int pi_pud;
		switch (pud) {
		case PULL_UP:
			pi_pud = PI_PUD_UP;
			break;
		case PULL_DOWN:
			pi_pud = PI_PUD_DOWN;
			break;
		default:
		case NONE:
			pi_pud = PI_PUD_OFF;
			break;
		}
		
		// wiringPi:
		gpioIntBuffer.put(GPPUD, pi_pud & 3);
		SleepUtil.busySleep(5_000);
	    gpioIntBuffer.put(GPIO_TO_PUDCLK[gpio], 1 << (gpio & 0x1F));
		SleepUtil.busySleep(5_000);
	    gpioIntBuffer.put(GPPUD, 0);
		SleepUtil.busySleep(5_000);
	    gpioIntBuffer.put(GPIO_TO_PUDCLK[gpio], 0);
		SleepUtil.busySleep(5_000);
	}
	
	@Override
	public boolean gpioRead(int gpio) {
		//return (gpioReg.get(GPLEV0 + (gpio >> 5)) & (1 << (gpio & 0x1F))) != 0;
		return (gpioIntBuffer.get(GPIO_TO_GPLEV[gpio]) & (1 << (gpio & 0x1F))) != 0;
	}
	
	@Override
	public void gpioWrite(int gpio, boolean value) {
		if (value) {
			// pigpio
			//gpioReg.put(GPSET0 + gpio >> 5, 1 << (gpio & 0x1F));
			// wiringPi
			gpioIntBuffer.put(GPIO_TO_GPSET[gpio], 1 << (gpio & 0x1F));
		} else {
			// pigpio
			//gpioReg.put(GPCLR0 + gpio >> 5, 1 << (gpio & 0x1F));
			// wiringPi
			gpioIntBuffer.put(GPIO_TO_GPCLR[gpio], 1 << (gpio & 0x1F));
		}
	}
}
