import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class GpioMMapTest {
	public static native MmapByteBuffer createMmapBuffer(String path, int offset, int length);
	public static native void closeMmapBuffer(int fd, int mapPtr, int length);
	
	static {
		System.loadLibrary("gpiommaptest");
	}
	
	//#define BANK (gpio>>5)
	//#define BIT  (1<<(gpio&0x1F))
	
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

	private static final int PI_INPUT = 0;
	private static final int PI_OUTPUT = 1;
	private static final int PUD_NONE = 0;
	
	private static IntBuffer gpioReg;
	
	public static void main(String[] args) {
		MmapByteBuffer mmap = createMmapBuffer("/dev/gpiomem", 0, GPIOMEM_LEN);
		if (mmap != null) {
			gpioReg = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			int gpio = 12;
			System.out.println("mode for pin " + gpio + "=" + getMode(gpio));
			setMode(gpio, PI_OUTPUT);
			System.out.println("mode for pin " + gpio + "=" + getMode(gpio));
			boolean on = read(gpio);
			System.out.println("on=" + on);
			System.out.println("Turning on");
			write(gpio, true);
			on = read(gpio);
			System.out.println("on=" + on);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Ignore
			}
			System.out.println("Turning off");
			write(gpio, false);
			on = read(gpio);
			System.out.println("on=" + on);
			long start = System.currentTimeMillis();
			int iterations = 40_000_000;
			for (int i=0; i<iterations; i++) {
				write(gpio, true);
				write(gpio, false);
			}
			double duration = (double) System.currentTimeMillis() - start;
			System.out.println("Took " + duration + "ms for " + iterations + " iterations, frequency=" + (iterations/duration) + "kHz");
			closeMmapBuffer(mmap.getFd(), mmap.getAddress(), mmap.getLength());
		}
	}
	
	private static int getMode(int gpio) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;
		
		return (gpioReg.get(reg) >> shift) & 7;
	}
	
	private static void setMode(int gpio, int mode) {
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
	
	private static int getPullUpDown(int gpio) {
		// TODO Implementation
		return PUD_NONE;
	}
	
	private static void setPullUpDown(int gpio, int pud) {
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
	
	private static boolean read(int gpio) {
		//return (gpioReg.get(GPLEV0 + (gpio >> 5)) & (1 << (gpio & 0x1F))) != 0;
		return (gpioReg.get(GPIO_TO_GPLEV[gpio])  & (1 << (gpio & 0x1F))) != 0;
	}
	
	private static void write(int gpio, boolean value) {
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
