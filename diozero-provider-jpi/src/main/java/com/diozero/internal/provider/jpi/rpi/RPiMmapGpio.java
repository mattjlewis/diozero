package com.diozero.internal.provider.jpi.rpi;

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

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.jpi.MmapGpioInterface;
import com.diozero.util.MmapBufferNative;
import com.diozero.util.MmapByteBuffer;

public class RPiMmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/gpiomem";
	private static final int GPIOMEM_LEN = 0xB4;
	// Offset to the GPIO Input level registers for each GPIO pin
	private static final byte[] GPIO_TO_GPLEV = {
			13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
			14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14
	};
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
	private IntBuffer gpioReg;
	
	@Override
	public synchronized void initialise() {
		if (! initialised) {
			mmap = MmapBufferNative.createMmapBuffer(GPIOMEM_DEVICE, 0, GPIOMEM_LEN);
			gpioReg = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			
			initialised = true;
		}
	}
	
	@Override
	public synchronized void terminate() {
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
	public int getMode(int gpio) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;

		// FIXME Map to DeviceMode enum
		return (gpioReg.get(reg) >> shift) & 7;
	}
	
	@Override
	public void setMode(int gpio, DeviceMode mode) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;
		
		switch (mode) {
		case DIGITAL_INPUT:
			gpioReg.put(reg, gpioReg.get(reg) & ~(7 << shift));
			break;
		case DIGITAL_OUTPUT:
			gpioReg.put(reg, (gpioReg.get(reg) & ~(7 << shift)) | (1 << shift));
			break;
		default:
			throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}
	
	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
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
		try {
			gpioReg.put(GPPUD, pi_pud & 3);
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
	
	@Override
	public boolean gpioRead(int gpio) {
		//return (gpioReg.get(GPLEV0 + (gpio >> 5)) & (1 << (gpio & 0x1F))) != 0;
		return (gpioReg.get(GPIO_TO_GPLEV[gpio]) & (1 << (gpio & 0x1F))) != 0;
	}
	
	@Override
	public void gpioWrite(int gpio, boolean value) {
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
