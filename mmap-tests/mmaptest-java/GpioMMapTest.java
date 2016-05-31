import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GpioMMapTest {
	private static final boolean READ_ONLY = true;
	private static final int SIZE_OF_INT = 4;
	
	static {
		System.loadLibrary("gpiommaptest");
	}

	private static final int PI_MAX_GPIO = 53;
	private static final int GPIO_LEN = 0xB4;

	/*
 	0 GPFSEL0   GPIO Function Select 0
 	1 GPFSEL1   GPIO Function Select 1
 	2 GPFSEL2   GPIO Function Select 2
 	3 GPFSEL3   GPIO Function Select 3
 	4 GPFSEL4   GPIO Function Select 4
 	5 GPFSEL5   GPIO Function Select 5
 	6 -         Reserved
 	7 GPSET0    GPIO Pin Output Set 0
 	8 GPSET1    GPIO Pin Output Set 1
 	9 -         Reserved
	10 GPCLR0    GPIO Pin Output Clear 0
	11 GPCLR1    GPIO Pin Output Clear 1
	12 -         Reserved
	13 GPLEV0    GPIO Pin Level 0
	14 GPLEV1    GPIO Pin Level 1
	15 -         Reserved
	16 GPEDS0    GPIO Pin Event Detect Status 0
	17 GPEDS1    GPIO Pin Event Detect Status 1
	18 -         Reserved
	19 GPREN0    GPIO Pin Rising Edge Detect Enable 0
	20 GPREN1    GPIO Pin Rising Edge Detect Enable 1
	21 -         Reserved
	22 GPFEN0    GPIO Pin Falling Edge Detect Enable 0
	23 GPFEN1    GPIO Pin Falling Edge Detect Enable 1
	24 -         Reserved
	25 GPHEN0    GPIO Pin High Detect Enable 0
	26 GPHEN1    GPIO Pin High Detect Enable 1
	27 -         Reserved
	28 GPLEN0    GPIO Pin Low Detect Enable 0
	29 GPLEN1    GPIO Pin Low Detect Enable 1
	30 -         Reserved
	31 GPAREN0   GPIO Pin Async. Rising Edge Detect 0
	32 GPAREN1   GPIO Pin Async. Rising Edge Detect 1
	33 -         Reserved
	34 GPAFEN0   GPIO Pin Async. Falling Edge Detect 0
	35 GPAFEN1   GPIO Pin Async. Falling Edge Detect 1
	36 -         Reserved
	37 GPPUD     GPIO Pin Pull-up/down Enable
	38 GPPUDCLK0 GPIO Pin Pull-up/down Enable Clock 0
	39 GPPUDCLK1 GPIO Pin Pull-up/down Enable Clock 1
	40 -         Reserved
	41 -         Test
	*/

	private static final int GPSET0 = 7;
	private static final int GPSET1 = 8;

	private static final int GPCLR0 = 10;
	private static final int GPCLR1 = 11;

	private static final int GPLEV0 = 13;
	private static final int GPLEV1 = 14;

	private static final int PI_OFF = 0;
	private static final int PI_ON = 1;

	private static final int PI_CLEAR = 0;
	private static final int PI_SET = 1;

	private static final int PI_LOW = 0;
	private static final int PI_HIGH = 1;

	private static final int PI_INPUT = 0;
	private static final int PI_OUTPUT = 1;

	private static final int PI_ALT0 = 4;
	private static final int PI_ALT1 = 5;
	private static final int PI_ALT2 = 6;
	private static final int PI_ALT3 = 7;
	private static final int PI_ALT4 = 3;
	private static final int PI_ALT5 = 2;

	private static final int PI_PUD_OFF = 0;
	private static final int PI_PUD_DOWN = 1;
	private static final int PI_PUD_UP = 2;

	public static native ByteBuffer initialise();
	public static native void test();
	public static native void terminate();

	//static volatile uint32_t * gpioReg = MAP_FAILED;
	private static ByteBuffer gpioReg = null;

	public static void main(String[] args) {
		int gpio = 12;

		try {
			gpioReg = initialise();
			if (gpioReg == null) {
				System.out.println("Error in initialise");
				return;
			}
			System.out.println("capacity=" + gpioReg.capacity());

			System.out.println("order=" + gpioReg.order());
			gpioReg.order(ByteOrder.LITTLE_ENDIAN);
			System.out.println("order=" + gpioReg.order());

			for (int i=0; i<20; i++) {
				System.out.format("gpioReg[%d]=0x%x%n", i, gpioReg.getInt(i*4));
			}
			for (int i=0; i<20; i++) {
				System.out.format("gpioReg[%d]=0x", i);
				for (int j=3; j>=0; j--) {
					System.out.format("%02x", gpioReg.get(i*4+j));
				}
				System.out.println();
			}

			test();
		
			for (int i=0; i<gpioReg.capacity()/SIZE_OF_INT; i+=4) {
				if ((i % 0x1000) == 0) {
					System.out.format("gpioReg[%d]=0x%x%n", i, gpioReg.getInt(i));
				}
				if (gpioReg.getInt(i) == 0x24024) {
					System.out.format("gpioReg[0x%x]=0x%x%n", i, gpioReg.getInt(i));
					System.out.println("Found it...");
					break;
				}
			}

			int rc = gpioSetMode(gpio, PI_OUTPUT);
			if (rc < 0) {
				System.out.println("Error in gpioSetMode()");
				return;
			}
			delay(1000);

			System.out.println("Turning GPIO " + gpio + " on");
			rc = gpioWrite(gpio, PI_ON);
			if (rc < 0) {
				System.out.println("Error in gpioWrite()");
				return;
			}
			delay(1000);

			System.out.println("Turning GPIO " + gpio + " off");
			rc = gpioWrite(gpio, PI_OFF);
			if (rc < 0) {
				System.out.println("Error in gpioWrite()");
				return;
			}
			delay(1000);
		} finally {
			gpioReg = null;
			terminate();
		}
	}

	public static int gpioSetMode(int gpio, int mode) {
		System.out.println("gpioSetMode(" + gpio + ", " + mode + ")");

		if (gpio > PI_MAX_GPIO || gpio < 0) {
			return -1;
		}

		if (mode > PI_ALT3 || mode < 0) {
			return -2;
		}

		int reg = gpio / 10;
		int shift = (gpio%10) * 3;

		int old_reg_val = gpioReg.getInt(reg*4);
		int old_mode = (old_reg_val >> shift) & 7;
		System.out.println("reg=" + reg + ", shift=" + shift + ", old_reg_val=" + old_reg_val + ", old_mode=" + old_mode);

		if (mode != old_mode) {
			//switchFunctionOff(gpio);
			int new_mode = (old_reg_val & ~(7<<shift)) | (mode<<shift);
			System.out.println("new_mode=" + new_mode);
			
			if (READ_ONLY) {
				System.out.println("READ_ONLY flag is true, not writing anything");
				return -1;
			}
			
			gpioReg.putInt(reg, new_mode);
		}

		return 0;
	}

	public static int gpioWrite(int gpio, int level) {
		if (gpio > PI_MAX_GPIO || gpio < 0) {
			return -1;
		}

		if (level > PI_ON || level < 0) {
			return -2;
		}

		int BIT = 1 << (gpio & 0x1F);
		int BANK = gpio >> 5;

		if (READ_ONLY) {
			System.out.println("READ_ONLY flag is true, not writing anything");
			return -3;
		}

		if (level == PI_OFF) {
			gpioReg.putInt(GPCLR0 + BANK, BIT);
		} else {
			gpioReg.putInt(GPSET0 + BANK, BIT);
		}

		return 0;
	}

	public static void delay(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}
