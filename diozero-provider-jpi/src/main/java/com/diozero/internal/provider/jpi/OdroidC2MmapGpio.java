package com.diozero.internal.provider.jpi;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.util.LibraryLoader;
import com.diozero.util.SleepUtil;

/**
 * See <a href="https://github.com/hardkernel/wiringPi/blob/master/wiringPi/wiringPi.c">Odroid wiringPi</a> fork.
 */
public class OdroidC2MmapGpio {
	private static final String MEM_DEVICE = "/dev/mem";
	private static final int GPIO_BASE_OFFSET = 0xC8834000;
	private static final int BLOCK_SIZE = 4*1024;
	private static final int C2_GPIO_PIN_BASE = 136;
	private static final int C2_GPIOY_PIN_START = C2_GPIO_PIN_BASE + 75;
	private static final int C2_GPIOX_PIN_START = C2_GPIO_PIN_BASE + 92;
	private static final int C2_GPIOX_PIN_END = C2_GPIO_PIN_BASE + 114;

	private static final int C2_GPIOX_FSEL_REG_OFFSET = 0x118;
	private static final int C2_GPIOX_OUTP_REG_OFFSET = 0x119;
	private static final int C2_GPIOX_INP_REG_OFFSET = 0x11A;
	private static final int C2_GPIOX_PUPD_REG_OFFSET = 0x13E;
	private static final int C2_GPIOX_PUEN_REG_OFFSET = 0x14C;

	private static final int C2_GPIOY_FSEL_REG_OFFSET = 0x10F;
	private static final int C2_GPIOY_OUTP_REG_OFFSET = 0x110;
	private static final int C2_GPIOY_INP_REG_OFFSET = 0x111;
	private static final int C2_GPIOY_PUPD_REG_OFFSET = 0x13B;
	private static final int C2_GPIOY_PUEN_REG_OFFSET = 0x149;
	
	private static final int[] C2_GP_TO_SHIFT_REG = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 };
	
	private static boolean loaded;
	private static MmapByteBuffer mmap;
	private static IntBuffer gpioReg;
	
	public static synchronized void initialise() {
		if (! loaded) {
			LibraryLoader.loadLibrary(JPiMmapGpio.class, "jpi");
			
			mmap = JPiNative.createMmapBuffer(MEM_DEVICE, GPIO_BASE_OFFSET, BLOCK_SIZE);
			gpioReg = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			
			loaded = true;
		}
	}
	
	public static synchronized void terminate() {
		if (loaded) {
			JPiNative.closeMmapBuffer(mmap.getFd(), mmap.getAddress(), mmap.getLength());
		}
	}
	
	public static int getMode(int gpio) {
		int reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET : C2_GPIOX_FSEL_REG_OFFSET;
		int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		
		return (gpioReg.get(reg) >> shift) & 1;
	}
	
	public static void setMode(int gpio, GpioDeviceInterface.Direction mode) {
		int reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_FSEL_REG_OFFSET : C2_GPIOX_FSEL_REG_OFFSET;
		int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		switch (mode) {
		case INPUT:
			gpioReg.put(reg, gpioReg.get(reg) | (1 << shift));
			break;
		case OUTPUT:
			gpioReg.put(reg, gpioReg.get(reg) & ~(1 << shift));
			break;
		default:
			throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}
	
	public static void setPullUpDown(int gpio, GpioPullUpDown pud) {
		int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		int pud_en_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_PUEN_REG_OFFSET : C2_GPIOX_PUEN_REG_OFFSET;
		if (pud == GpioPullUpDown.NONE) {
			// Disable Pull/Pull-down resister
			gpioReg.put(pud_en_reg, gpioReg.get(pud_en_reg) & ~(1 << shift));
		} else {
			// Enable Pull/Pull-down resister
			gpioReg.put(pud_en_reg, gpioReg.get(pud_en_reg) | (1 << shift));
			int pud_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_PUPD_REG_OFFSET : C2_GPIOX_PUPD_REG_OFFSET;
			if (pud == GpioPullUpDown.PULL_UP) {
				gpioReg.put(pud_reg, gpioReg.get(pud_reg) |  (1 << shift));
			} else {
				gpioReg.put(pud_reg, gpioReg.get(pud_reg) & ~(1 << shift));
			}
		}
	}
	
	public static boolean gpioRead(int gpio) {
		int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		int gp_lev_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_INP_REG_OFFSET : C2_GPIOX_INP_REG_OFFSET;
		return gpioReg.get(gp_lev_reg & (1 << shift)) != 0;
	}
	
	public static void gpioWrite(int gpio, boolean value) {
		int shift = C2_GP_TO_SHIFT_REG[gpio - C2_GPIOY_PIN_START];
		int gp_set_reg = gpio < C2_GPIOX_PIN_START ? C2_GPIOY_OUTP_REG_OFFSET : C2_GPIOX_OUTP_REG_OFFSET;
		if (value) {
			gpioReg.put(gp_set_reg, gpioReg.get(gp_set_reg) | (1 << shift));
		} else {
			gpioReg.put(gp_set_reg, gpioReg.get(gp_set_reg) & ~(1 << shift));
		}
	}
	
	public static void main(String[] args) {
		initialise();
		
		if (args.length != 2) {
			System.out.println("Usage: " + OdroidC2MmapGpio.class.getName() + " <gpio> <iterations>");
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);
		int iterations = Integer.parseInt(args[1]);
		
		setMode(gpio, GpioDeviceInterface.Direction.OUTPUT);

		for (int i=0; i<5; i++) {
			System.out.println("on");
			gpioWrite(gpio, true);
			SleepUtil.sleepSeconds(1);
			System.out.println("off");
			gpioWrite(gpio, false);
			SleepUtil.sleepSeconds(1);
		}
		
		long start = System.currentTimeMillis();
		for (int i=0; i<iterations; i++) {
			gpioWrite(gpio, true);
			gpioWrite(gpio, false);
		}
		double duration = System.currentTimeMillis() - start;
		System.out.println("Took " + duration + "ms for " + iterations + " iterations, frequency=" + (iterations/duration) + "kHz");
		terminate();
	}
}
