package com.diozero.internal.provider.jpi;

/*
 * #%L
 * Device I/O Zero - Java Native provider for the Raspberry Pi
 * %%
 * Copyright (C) 2016 mattjlewis
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

import com.diozero.util.LibraryLoader;

public class JPiNative {
	private static native MmapByteBuffer createMmapBuffer(String path, int offset, int length);
	private static native void closeMmapBuffer(int fd, int mapPtr, int length);
	
	private static final String GPIOMEM_DEVICE = "/dev/gpiomem";
	private static final int GPIOMEM_LEN = 0xB4;
	// Offset to the GPIO Input level registers for each GPIO pin
	private static final byte GPLEV0 = 13;
	private static final byte GPLEV1 = 14;
	private static final byte[] GPIO_TO_GPLEV = {
			13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
			14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14
	};
	// Offset to the GPIO Set registers for each GPIO pin
	private static final byte GPSET0 = 7;
	private static final byte GPSET1 = 8;
	private static final byte[] GPIO_TO_GPSET = {
			7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
			8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
	};
	// Offset to the GPIO Clear registers for each GPIO pin
	private static final byte GPCLR0 = 10;
	private static final byte GPCLR1 = 11;
	private static final byte[] GPIO_TO_GPCLR = {
			10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,
			11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11
	};
	// GPIO Pin pull up/down register
	private static final byte GPPUD = 37;
	// Offset to the Pull Up Down Clock register
	private static final byte GPPUDCLK0 = 38;
	private static final byte GPPUDCLK1 = 39;
	private static byte[] GPIO_TO_PUDCLK = {
			38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,38,
			39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39,39
	};
	
	private static final int PI_LOW = 0;
	private static final int PI_HIGH = 1;

	public static final int PI_INPUT = 0;
	public static final int PI_OUTPUT = 1;
	
	public static final int PI_PUD_OFF = 0;
	public static final int PI_PUD_DOWN = 1;
	public static final int PI_PUD_UP = 2;
	
	private static boolean loaded;
	private static MmapByteBuffer mmap;
	private static IntBuffer gpioReg;
	
	public static synchronized void initialise() {
		if (! loaded) {
			LibraryLoader.loadLibrary(JPiNative.class, "jpi");
			
			mmap = createMmapBuffer(GPIOMEM_DEVICE, 0, GPIOMEM_LEN);
			gpioReg = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			
			loaded = true;
		}
	}
	
	public static void terminate() {
		closeMmapBuffer(mmap.getFd(), mmap.getAddress(), mmap.getLength());
	}
	
	/** Returns the function of a GPIO: 0=input, 1=output, 4=alt0 */
	public static int getMode(int gpio) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;
		
		return (gpioReg.get(reg) >> shift) & 7;
	}
	
	public static void setMode(int gpio, int mode) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;
		
		if (mode == PI_INPUT) {
			gpioReg.put(reg, gpioReg.get(reg) & ~(7 << shift));
		} else if (mode == PI_OUTPUT) {
			gpioReg.put(reg, (gpioReg.get(reg) & ~(7 << shift)) | (1 << shift));
		} else {
			throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}
	
	public static void setPullUpDown(int gpio, int pud) {
		// pigpio:
		/*
		try {
			gpioReg.put(GPPUD, pud);
			// Sleep 20us
			Thread.sleep(0, 20_000);
			gpioReg.put(GPPUDCLK0 + gpio >> 5, 1 << (gpio & 0x1F));
			Thread.sleep(0, 20_000);
			gpioReg.put(GPPUD, 0);
			gpioReg.put(GPPUDCLK0 + gpio >> 5, 0);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
		*/
		
		// wiringPi:
		try {
			gpioReg.put(GPPUD, pud & 3);
			Thread.sleep(0, 5_000);
		    gpioReg.put(GPIO_TO_PUDCLK[gpio], 1 << (gpio & 0x1F));
			Thread.sleep(0, 5_000);
		    gpioReg.put(GPPUD, 0);
			Thread.sleep(0, 5_000);
		    gpioReg.put(GPIO_TO_PUDCLK[gpio], 0);
			Thread.sleep(0, 5_000);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
	}
	
	public static boolean gpioRead(int gpio) {
		//return (gpioReg.get(GPLEV0 + (gpio >> 5)) & (1 << (gpio & 0x1F))) != 0;
		return (gpioReg.get(GPIO_TO_GPLEV[gpio])  & (1 << (gpio & 0x1F))) != 0;
	}
	
	public static void gpioWrite(int gpio, boolean value) {
		if (value) {
			// pigpio
			//gpioReg.put(GPSET0 + gpio >> 5, 1 << (gpio & 0x1F));
			// wiringPi
			gpioReg.put(GPIO_TO_GPSET[gpio], 1 << (gpio & 0x1F));
		} else {
			// pigpio
			//gpioReg.put(GPCLR0 + gpio >> 5, 1 << (gpio & 0x1F));
			// wiringPi
			gpioReg.put(GPIO_TO_GPCLR[gpio], 1 << (gpio & 0x1F));
		}
	}
}
