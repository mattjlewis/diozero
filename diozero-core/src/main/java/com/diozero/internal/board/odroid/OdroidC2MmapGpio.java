package com.diozero.internal.board.odroid;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     OdroidC2MmapGpio.java  
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Random;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InvalidModeException;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.Hex;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/**
 * See <a href="https://github.com/hardkernel/wiringPi/blob/master/wiringPi/wiringPi.c">Odroid wiringPi</a> fork.
 */
public class OdroidC2MmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";
	
	private static final int C2_GPIO_BASE = 0xC8834000;
	private static final int BLOCK_SIZE = 4*1024;
	
	private static final int C2_GPIO_PIN_BASE = 136;
	
	// XXX Armbian Kernel 5.x has these starting at an offset of 242 (0xF2)
	private static final int MY_OFFSET_HACK = 242;
	//private static final int MY_OFFSET_HACK = 0;
	
	private static final int C2_GPIODV_PIN_START = C2_GPIO_PIN_BASE + 45 + MY_OFFSET_HACK;
	private static final int C2_GPIODV_PIN_END = C2_GPIO_PIN_BASE + 74 + MY_OFFSET_HACK;
	
	private static final int C2_GPIOY_PIN_START = C2_GPIO_PIN_BASE + 75 + MY_OFFSET_HACK;
	private static final int C2_GPIOY_PIN_END = C2_GPIO_PIN_BASE + 91 + MY_OFFSET_HACK;

	private static final int C2_GPIOX_PIN_START = C2_GPIO_PIN_BASE + 92 + MY_OFFSET_HACK;
	private static final int C2_GPIOX_PIN_END = C2_GPIO_PIN_BASE + 114 + MY_OFFSET_HACK;

	private static final int C2_GPIODV_FSEL_REG_OFFSET = 0x10C;
	private static final int C2_GPIODV_OUTP_REG_OFFSET = 0x10D;
	private static final int C2_GPIODV_INP_REG_OFFSET = 0x10E;
	private static final int C2_GPIODV_PUPD_REG_OFFSET = 0x148;
	private static final int C2_GPIODV_PUEN_REG_OFFSET = 0x13A;
	
	private static final int C2_GPIOY_FSEL_REG_OFFSET = 0x10F;
	private static final int C2_GPIOY_OUTP_REG_OFFSET = 0x110;
	private static final int C2_GPIOY_INP_REG_OFFSET = 0x111;
	private static final int C2_GPIOY_PUPD_REG_OFFSET = 0x13B;
	private static final int C2_GPIOY_PUEN_REG_OFFSET = 0x149;
	
	private static final int C2_GPIOX_FSEL_REG_OFFSET = 0x118;
	private static final int C2_GPIOX_OUTP_REG_OFFSET = 0x119;
	private static final int C2_GPIOX_INP_REG_OFFSET = 0x11A;
	private static final int C2_GPIOX_PUPD_REG_OFFSET = 0x13E;
	private static final int C2_GPIOX_PUEN_REG_OFFSET = 0x14C;
	
	private static final int C2_MUX_REG_0_OFFSET = 0x12C;
	private static final int C2_MUX_REG_1_OFFSET = 0x12D;
	private static final int C2_MUX_REG_2_OFFSET = 0x12E;
	private static final int C2_MUX_REG_3_OFFSET = 0x12F;
	private static final int C2_MUX_REG_4_OFFSET = 0x130;
	private static final int C2_MUX_REG_5_OFFSET = 0x131;
	private static final int C2_MUX_REG_7_OFFSET = 0x133;
	private static final int C2_MUX_REG_8_OFFSET = 0x134;
	
	//private static final int[] C2_GP_TO_SHIFT_REG = new int[] { //
	//		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, //
	//		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 };
	
	private boolean initialised;
	private MmapIntBuffer mmapIntBuffer;
	//private MmapByteBuffer mmap;
	//private volatile IntBuffer gpioIntBuffer;
	
	@Override
	public synchronized void initialise() {
		if (! initialised) {
			//mmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, C2_GPIO_BASE, BLOCK_SIZE);
			//gpioIntBuffer = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			mmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, C2_GPIO_BASE, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			
			initialised = true;
		}
	}
	
	@Override
	public synchronized void close() {
		if (initialised) {
			//MmapBufferNative.closeMmapBuffer(mmap.getFd(), mmap.getAddress(), mmap.getLength());
			mmapIntBuffer.close();
		}
	}
	
	/*
	 * Offset to the GPIO Set register
	 */
	private static int gpioToGPSETReg(int gpio) {
		if (gpio >= C2_GPIOX_PIN_START && gpio <= C2_GPIOX_PIN_END) {
			return C2_GPIOX_OUTP_REG_OFFSET;
		}
		if (gpio >= C2_GPIOY_PIN_START && gpio <= C2_GPIOY_PIN_END) {
			return C2_GPIOY_OUTP_REG_OFFSET;
		}
		if (gpio >= C2_GPIODV_PIN_START && gpio <= C2_GPIODV_PIN_END) {
			return  C2_GPIODV_OUTP_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Input register
	 */
	private static final int gpioToGPLEVReg(int gpio) {
		if (gpio >= C2_GPIOX_PIN_START && gpio <= C2_GPIOX_PIN_END) {
			return C2_GPIOX_INP_REG_OFFSET;
		}
		if (gpio >= C2_GPIOY_PIN_START && gpio <= C2_GPIOY_PIN_END) {
			return C2_GPIOY_INP_REG_OFFSET;
		}
		if (gpio >= C2_GPIODV_PIN_START && gpio <= C2_GPIODV_PIN_END) {
			return  C2_GPIODV_INP_REG_OFFSET;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Pull up/down enable register
	 */
	private static int gpioToPUENReg (int gpio) {
		if (gpio >= C2_GPIOX_PIN_START && gpio <= C2_GPIOX_PIN_END) {
			return  C2_GPIOX_PUEN_REG_OFFSET;
		}
		if (gpio >= C2_GPIOY_PIN_START && gpio <= C2_GPIOY_PIN_END) {
			return  C2_GPIOY_PUEN_REG_OFFSET;
		}
		if (gpio >= C2_GPIODV_PIN_START && gpio <= C2_GPIODV_PIN_END) {
			return  C2_GPIODV_PUEN_REG_OFFSET;
		}
		return	-1;
	}

	/*
	 * Offset to the GPIO Pull up/down register
	 */
	private static int gpioToPUPDReg (int gpio) {
		if (gpio >= C2_GPIOX_PIN_START && gpio <= C2_GPIOX_PIN_END) {
			return	C2_GPIOX_PUPD_REG_OFFSET;
		}
		if (gpio >= C2_GPIOY_PIN_START && gpio <= C2_GPIOY_PIN_END) {
			return  C2_GPIOY_PUPD_REG_OFFSET;
		}
		if (gpio >= C2_GPIODV_PIN_START && gpio <= C2_GPIODV_PIN_END) {
			return  C2_GPIODV_PUPD_REG_OFFSET;
		}
		return	-1;
	}

	/*
	 * Offset to the GPIO bit
	 */
	private static int gpioToShiftReg(int gpio) {
		if (gpio >= C2_GPIOX_PIN_START && gpio <= C2_GPIOX_PIN_END) {
			return gpio - C2_GPIOX_PIN_START;
		}
		if (gpio >= C2_GPIOY_PIN_START && gpio <= C2_GPIOY_PIN_END) {
			return gpio - C2_GPIOY_PIN_START;
		}
		if (gpio >= C2_GPIODV_PIN_START && gpio <= C2_GPIODV_PIN_END) {
			return  gpio - C2_GPIODV_PIN_START;
		}
		return -1;
	}

	/*
	 * Offset to the GPIO Function register
	 */
	private static final int gpioToGPFSELReg(int gpio) {
		if (gpio >= C2_GPIOX_PIN_START && gpio <= C2_GPIOX_PIN_END) {
			return C2_GPIOX_FSEL_REG_OFFSET;
		}
		if (gpio >= C2_GPIOY_PIN_START && gpio <= C2_GPIOY_PIN_END) {
			return C2_GPIOY_FSEL_REG_OFFSET;
		}
		if (gpio >= C2_GPIODV_PIN_START && gpio <= C2_GPIODV_PIN_END) {
			return  C2_GPIODV_FSEL_REG_OFFSET;
		}
		return -1;
	}

	@Override
	public DeviceMode getMode(int gpio) {
		int fsel = gpioToGPFSELReg(gpio);
		int shift = gpioToShiftReg(gpio);
		//int fsel = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET : C2_GPIOX_FSEL_REG_OFFSET;
		//int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		
		if (fsel == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return DeviceMode.UNKNOWN;
		}
		
		//return (gpioReg.get(fsel) & (1 << shift)) == 0 ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT;
		//switch (gpioIntBuffer.get(fsel) & (1 << shift)) {
		if (mmapIntBuffer.get(fsel, 1 << shift) == 0) {
			return DeviceMode.DIGITAL_OUTPUT;
		}

		return DeviceMode.DIGITAL_INPUT;
	}
	
	@Override
	public void setMode(int gpio, DeviceMode mode) {
		int fsel = gpioToGPFSELReg(gpio);
		int shift = gpioToShiftReg(gpio);
		//int fsel = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET : C2_GPIOX_FSEL_REG_OFFSET;
		//int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		
		if (fsel == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return;
		}
		
		switch (mode) {
		case DIGITAL_INPUT:
			//gpioIntBuffer.put(fsel, gpioIntBuffer.get(fsel) | (1 << shift));
			//*(gpio + fsel) = (*(gpio + fsel) | (1 << shift));
			mmapIntBuffer.put(fsel, mmapIntBuffer.get(fsel) | (1 << shift));
			//_pullUpDnControl(origPin, PUD_OFF);
			//_pullUpDnControl(origPin, PUD_ON);
			break;
		case DIGITAL_OUTPUT:
			//gpioIntBuffer.put(fsel, gpioIntBuffer.get(fsel) & ~(1 << shift));
			//*(gpio + fsel) = (*(gpio + fsel) & ~(1 << shift));
			mmapIntBuffer.put(fsel, mmapIntBuffer.get(fsel) & ~(1 << shift));
			break;
		default:
			throw new InvalidModeException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}
	
	public int getPullUpDown(int gpio) {
		int puen = gpioToPUENReg(gpio);
		int shift = gpioToShiftReg(gpio);
		
		if (puen == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return -1;
		}
		
		if ((mmapIntBuffer.get(puen) & (1 << shift)) != 0) {
			int pupd = gpioToPUPDReg(gpio);
			return (mmapIntBuffer.get(pupd) & (1 << shift)) == 0 ? 2 : 1;
		}
		return 0;
	}
	
	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		int shift = gpioToShiftReg(gpio);
		int puen = gpioToPUENReg(gpio);
		//int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		//int puen = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_PUEN_REG_OFFSET : C2_GPIOX_PUEN_REG_OFFSET;
		
		if (puen == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return;
		}
		
		if (pud == GpioPullUpDown.NONE) {
			// Disable Pull/Pull-down resister
			//gpioIntBuffer.put(puen, gpioIntBuffer.get(puen) & ~(1 << shift));
			//*(gpio + puen) = (*(gpio + puen) & ~(1 << shift));
			mmapIntBuffer.put(puen, mmapIntBuffer.get(puen) & ~(1 << shift));
		} else {
			// Enable Pull/Pull-down resister
			//gpioIntBuffer.put(puen, gpioIntBuffer.get(puen) | (1 << shift));
			//*(gpio + puen) = (*(gpio + puen) | (1 << shift));
			mmapIntBuffer.put(puen, mmapIntBuffer.get(puen) | (1 << shift));
			
			int pupd = gpioToPUPDReg(gpio);
			//int pupd = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_PUPD_REG_OFFSET : C2_GPIOX_PUPD_REG_OFFSET;
			if (pud == GpioPullUpDown.PULL_UP) {
				//gpioIntBuffer.put(pupd, gpioIntBuffer.get(pupd) |  (1 << shift));
				//*(gpio + pupd) = (*(gpio + pupd) |  (1 << shift));
				mmapIntBuffer.put(pupd, mmapIntBuffer.get(pupd) |  (1 << shift));
			} else {
				//gpioIntBuffer.put(pupd, gpioIntBuffer.get(pupd) & ~(1 << shift));
				//*(gpio + pupd) = (*(gpio + pupd) & ~(1 << shift));
				mmapIntBuffer.put(pupd, mmapIntBuffer.get(pupd) & ~(1 << shift));
			}
		}
	}
	
	@Override
	public boolean gpioRead(int gpio) {
		int reg = gpioToGPLEVReg(gpio);
		int shift = gpioToShiftReg(gpio);
		//int reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_INP_REG_OFFSET : C2_GPIOX_INP_REG_OFFSET;
		//int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		
		if (reg == -1 || shift == -1) {
			Logger.error("Invalid GPIO {}", Integer.valueOf(gpio));
			return false;
		}

		//return (gpioIntBuffer.get(gp_lev_reg) & (1 << shift)) != 0;
		/*-
		if ((*(gpio + reg) & (1 << shift)) != 0)
			return HIGH;
		else
			return LOW;
		 */
		return (mmapIntBuffer.get(reg) & (1 << shift)) != 0;
	}
	
	@Override
	public void gpioWrite(int gpio, boolean value) {
		// Note no boundary checks to maximise performance
		
		int reg = gpioToGPLEVReg(gpio);
		int shift = gpioToShiftReg(gpio);
		//int reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_OUTP_REG_OFFSET : C2_GPIOX_OUTP_REG_OFFSET;
		//int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		
		if (value) {
			//gpioIntBuffer.put(reg, gpioIntBuffer.get(reg) | (1 << shift));
			//*(gpio + reg) |=  (1 << shift);
			mmapIntBuffer.put(reg, mmapIntBuffer.get(reg) | (1 << shift));
		} else {
			//gpioIntBuffer.put(reg, gpioIntBuffer.get(reg) & ~(1 << shift));
			//*(gpio + reg) &= ~(1 << shift);
			mmapIntBuffer.put(reg, mmapIntBuffer.get(reg) & ~(1 << shift));
		}
	}
	
	public static void main(String[] args) {
		System.out.println(ByteOrder.nativeOrder());
		if (args.length != 2) {
			System.out.println("Usage: " + OdroidC2MmapGpio.class.getName() + " <gpio> <iterations>");
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);
		int iterations = Integer.parseInt(args[1]);
		
		int gp_set_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_OUTP_REG_OFFSET : C2_GPIOX_OUTP_REG_OFFSET;
		int gp_set_reg2 = gpioToGPSETReg(gpio);
		System.out.println("gp_set_reg=" + gp_set_reg + ", gp_set_reg2=" + gp_set_reg2);
		//int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		//int shift2 = gpioToShiftReg(gpio);
		//System.out.println("shift=" + shift + ", shift2=" + shift2);
		int gp_lev_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_INP_REG_OFFSET : C2_GPIOX_INP_REG_OFFSET;
		int gp_lev_reg2 = gpioToGPLEVReg(gpio);
		System.out.println("gp_lev_reg=" + gp_lev_reg + ", gp_lev_reg2=" + gp_lev_reg2);
		int gp_fsel_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET : C2_GPIOX_FSEL_REG_OFFSET;
		int gp_fsel_reg2 = gpioToGPFSELReg(gpio);
		System.out.println("gp_fsel_reg=" + gp_fsel_reg + ", gp_fsel_reg2=" + gp_fsel_reg2);
		
		System.out.format("gpioToGPSETReg(%d)=0x%04x%n", Integer.valueOf(214), Integer.valueOf(gpioToGPSETReg(214)));
		System.out.format("gpioToGPSETReg(%d)=0x%04x%n", Integer.valueOf(219), Integer.valueOf(gpioToGPSETReg(219)));
		System.out.format("gpioToGPFSELReg(%d)=0x%04x%n", Integer.valueOf(214), Integer.valueOf(gpioToGPFSELReg(214)));
		System.out.format("gpioToGPFSELReg(%d)=0x%04x%n", Integer.valueOf(219), Integer.valueOf(gpioToGPFSELReg(219)));
		
		try (OdroidC2MmapGpio mmap_gpio = new OdroidC2MmapGpio()) {
			mmap_gpio.initialise();
	
			/*
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try {
				while (true) {
					Hex.dumpIntBuffer(mmap_gpio.gpioIntBuffer, C2_GPIO_PIN_BASE, 200);
					String line = reader.readLine();
					if (line == null || line.equals("q")) {
						break;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			
			System.out.println("getMode(" + gpio + ")=" + mmap_gpio.getMode(gpio));
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			System.out.println("getMode(" + gpio + ")=" + mmap_gpio.getMode(gpio));
	
			System.out.println("Current val=" + mmap_gpio.gpioRead(gpio));
			for (int i=0; i<5; i++) {
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
				long start = System.currentTimeMillis();
				for (int i=0; i<iterations; i++) {
					mmap_gpio.gpioWrite(gpio, true);
					mmap_gpio.gpioWrite(gpio, false);
				}
				long duration = System.currentTimeMillis() - start;
				System.out.format("Took %d ms for %d iterations, frequency=%.2fkHz%n",
						Long.valueOf(duration), Integer.valueOf(iterations), Double.valueOf(iterations/(double) duration));
			}
	
			for (int i=0; i<5; i++) {
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
		Random rand = new Random();
		IntBuffer buffer = ByteBuffer.allocateDirect(500).asIntBuffer();
		for (int i=0; i<buffer.capacity(); i++) {
			buffer.put(rand.nextInt());
		}
		buffer.flip();
		Hex.dumpIntBuffer(buffer, 0, 2);
	}
}
