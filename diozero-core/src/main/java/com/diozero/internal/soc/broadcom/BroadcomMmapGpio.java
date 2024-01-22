package com.diozero.internal.soc.broadcom;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BroadcomMmapGpio.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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
import java.util.Optional;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/**
 * https://datasheets.raspberrypi.org/bcm2835/bcm2835-peripherals.pdf
 */
public class BroadcomMmapGpio implements MmapGpioInterface {
	/*-
	 * The BCM2835 has 54 GPIO pins.
	 * BCM2835 data sheet, Page 90 onwards.
	 * There are 6 control registers, each control the functions of a block
	 * of 10 pins.
	 * Each control register has 10 sets of 3 bits per GPIO pin - the ALT values
	 * 000 = GPIO Pin X is an input
	 * 001 = GPIO Pin X is an output
	 * 100 = GPIO Pin X takes alternate function 0
	 * 101 = GPIO Pin X takes alternate function 1
	 * 110 = GPIO Pin X takes alternate function 2
	 * 111 = GPIO Pin X takes alternate function 3
	 * 011 = GPIO Pin X takes alternate function 4
	 * 010 = GPIO Pin X takes alternate function 5
	 */
	private static final int FSEL_INPT = 0b000;
	private static final int FSEL_OUTP = 0b001;
	private static final int FSEL_ALT0 = 0b100;
	private static final int FSEL_ALT1 = 0b101;
	private static final int FSEL_ALT2 = 0b110;
	private static final int FSEL_ALT3 = 0b111;
	private static final int FSEL_ALT4 = 0b011;
	private static final int FSEL_ALT5 = 0b010;

	// #define GPIO_BASE (pi_peri_phys + 0x00200000)
	// #define PCM_BASE (pi_peri_phys + 0x00203000)
	// #define SPI_BASE (pi_peri_phys + 0x00204000)
	// #define PWM_BASE (pi_peri_phys + 0x0020C000)

	private static final String GPIOMEM_DEVICE = "/dev/gpiomem";
	// private static final int GPIOMEM_LEN = 0xB4;
	private static final int GPIOMEM_LEN = 4096;

	// Offset to the GPIO Set registers for each GPIO pin
	private static final byte[] GPIO_TO_GPSET = { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
			8, 8, 8, 8, 8 };
	// Offset to the GPIO Clear registers for each GPIO pin
	private static final byte[] GPIO_TO_GPCLR = { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
			10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
			11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11 };
	// Offset to the GPIO Input level registers for each GPIO pin
	private static final byte[] GPIO_TO_GPLEV = { 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
			13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
			14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14 };

	/* BCM2711 has different pulls */
	private static final int GPPUPPDN0 = 57;
	/*-
	private static final int GPPUPPDN1 = 58;
	private static final int GPPUPPDN2 = 59;
	private static final int GPPUPPDN3 = 60;
	*/

	// GPIO Pin pull up/down register
	private static final byte GPPUD = 37;
	// Offset to the Pull Up Down Clock register
	private static byte[] GPIO_TO_PUDCLK = { 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
			38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
			39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39 };

	private static final int PI_2711_PUD_OFF = 0;
	private static final int PI_2711_PUD_DOWN = 2;
	private static final int PI_2711_PUD_UP = 1;

	private static final int PI_28XX_PUD_OFF = 0;
	private static final int PI_28XX_PUD_DOWN = 1;
	private static final int PI_28XX_PUD_UP = 2;

	private boolean initialised;
	private boolean piIs2711;
	private MmapIntBuffer mmapIntBuffer;

	public BroadcomMmapGpio(String soc) {
		this.piIs2711 = soc.equals(RaspberryPiBoardInfoProvider.BCM2711);
	}

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			// Note /dev/gpiomem device ignores any offset and always grants access to the
			// GPIO register area
			if (mmapIntBuffer == null) {
				mmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, 0, GPIOMEM_LEN, ByteOrder.LITTLE_ENDIAN);
			}

			initialised = true;
		}
	}

	@Override
	public synchronized void close() {
		if (initialised) {
			mmapIntBuffer.close();
			mmapIntBuffer = null;
		}
	}

	/**
	 * Returns the function of a GPIO: 0=input, 1=output, 4=alt0
	 *
	 * @param gpio GPIO number
	 * @return GPIO mode (0 - INPUT, 1 - OUTPUT)
	 */
	@Override
	public DeviceMode getMode(int gpio) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;

		/*-
		 * See BCM2385 data sheet page 102.
		 *
		 * PWM0 can be on GPIOs 12, 18, 40, 52
		 * PWM1 can on on GPIOs 13, 19, 41, 45, 53
		 * FSEL_ALT0 (func=4=0b100) for PWM output for pins 12, 13, 40, 41 and 45
		 * FSEL_ALT1 (func=5=0b101) for PWM output for pins 52 and 53
		 * FSEL_ALT5 (func=2=0b010) for PWM output for pins 18 and 19
		 */
		int mode = mmapIntBuffer.getShiftRight(reg, shift, 7);
		Logger.debug("mode for {}: {}", Integer.valueOf(gpio), Integer.valueOf(mode));
		DeviceMode device_mode = DeviceMode.UNKNOWN;
		switch (mode) {
		case FSEL_INPT:
			device_mode = DeviceMode.DIGITAL_INPUT;
			break;
		case FSEL_OUTP:
			device_mode = DeviceMode.DIGITAL_OUTPUT;
			break;
		case FSEL_ALT0:
			if (gpio == 12 || gpio == 13 || gpio == 40 || gpio == 41 || gpio == 45) {
				device_mode = DeviceMode.PWM_OUTPUT;
			} else if (gpio >= 0 && gpio < 4 || gpio >= 28 && gpio < 30) {
				device_mode = DeviceMode.I2C;
			} else if (gpio >= 7 && gpio < 12) {
				device_mode = DeviceMode.SPI;
			} else if (gpio >= 14 && gpio < 16) {
				device_mode = DeviceMode.SERIAL;
			}
			break;
		case FSEL_ALT1:
			if (gpio == 52 || gpio == 53) {
				device_mode = DeviceMode.PWM_OUTPUT;
			} else if (gpio == 44 || gpio == 45) {
				device_mode = DeviceMode.I2C;
			}
			break;
		case FSEL_ALT2:
			if (gpio >= 36 && gpio < 40) {
				device_mode = DeviceMode.SERIAL;
			} else if (gpio == 44 || gpio == 45) {
				device_mode = DeviceMode.I2C;
			}
			break;
		case FSEL_ALT3:
			if (gpio >= 16 && gpio < 18 || gpio >= 30 && gpio < 34) {
				device_mode = DeviceMode.SERIAL;
			}
			break;
		case FSEL_ALT4:
			if (gpio >= 16 && gpio < 22 || gpio >= 40 && gpio < 46) {
				device_mode = DeviceMode.SPI;
			}
			break;
		case FSEL_ALT5:
			if (gpio >= 14 && gpio < 18 || gpio >= 30 && gpio < 34 || gpio >= 40 && gpio < 44) {
				device_mode = DeviceMode.SERIAL;
			} else if (gpio == 18 || gpio == 19) {
				device_mode = DeviceMode.PWM_OUTPUT;
			}
			break;
		default:
			// Ignore
		}

		return device_mode;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			// mmapIntBuffer.put(reg, mmapIntBuffer.get(reg) & ~(0b111 << shift));
			setModeUnchecked(gpio, 0);
			break;
		case DIGITAL_OUTPUT:
			// mmapIntBuffer.put(reg, mmapIntBuffer.get(reg) & ~(0b111 << shift) | (1 <<
			// shift));
			setModeUnchecked(gpio, 1);
			break;
		case PWM_OUTPUT:
			int m_val;
			if (gpio == 12 || gpio == 13 || gpio == 40 || gpio == 41 || gpio == 45) {
				m_val = FSEL_ALT0;
			} else if (gpio == 52 || gpio == 53) {
				m_val = FSEL_ALT1;
			} else if (gpio == 18 || gpio == 19) {
				m_val = FSEL_ALT5;
			} else {
				throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
			}
			// mmapIntBuffer.put(reg, mmapIntBuffer.get(reg) & ~(7 << shift) | (m_val <<
			// shift));
			setModeUnchecked(gpio, m_val);
			break;
		default:
			throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}

	@Override
	public void setModeUnchecked(int gpio, int mode) {
		int reg = gpio / 10;
		int shift = (gpio % 10) * 3;

		mmapIntBuffer.put(reg, mmapIntBuffer.get(reg) & ~(0b111 << shift) | (mode << shift));
	}

	@Override
	public Optional<GpioPullUpDown> getPullUpDown(int gpio) {
		if (!piIs2711) {
			return Optional.empty();
		}

		int pud_int_offset = GPPUPPDN0 + (gpio >> 4);
		int pud_shift = (gpio & 0xf) << 1;
		GpioPullUpDown pud;
		switch ((mmapIntBuffer.get(pud_int_offset) >> pud_shift) & 0b11) {
		case PI_2711_PUD_UP:
			pud = GpioPullUpDown.PULL_UP;
			break;
		case PI_2711_PUD_DOWN:
			pud = GpioPullUpDown.PULL_DOWN;
			break;
		default:
			pud = GpioPullUpDown.NONE;
		}

		return Optional.of(pud);
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		// See pigpio: https://github.com/joan2937/pigpio/blob/master/pigpio.c#L8880

		if (piIs2711) {
			int pull;
			switch (pud) {
			case PULL_UP:
				pull = PI_2711_PUD_UP;
				break;
			case PULL_DOWN:
				pull = PI_2711_PUD_DOWN;
				break;
			case NONE:
			default:
				pull = PI_2711_PUD_OFF;
				break;
			}

			/*-
			 *
			bits = *(gpioReg + GPPUPPDN0 + (gpio>>4));
			bits &= ~(3 << shift);
			bits |= (pull << shift);
			*(gpioReg + GPPUPPDN0 + (gpio>>4)) = bits;
			*/
			int pud_int_offset = GPPUPPDN0 + (gpio >> 4);
			int pud_shift = (gpio & 0xf) << 1;
			int reg_val = mmapIntBuffer.get(pud_int_offset);
			reg_val &= ~(0b11 << pud_shift);
			reg_val |= (pull << pud_shift);
			mmapIntBuffer.put(pud_int_offset, reg_val);
		} else {
			int pull;
			switch (pud) {
			case PULL_UP:
				pull = PI_28XX_PUD_UP;
				break;
			case PULL_DOWN:
				pull = PI_28XX_PUD_DOWN;
				break;
			case NONE:
			default:
				pull = PI_28XX_PUD_OFF;
				break;
			}

			/*-
			#define BANK (gpio >> 5)
			#define BIT  (1 << (gpio & 0x1F))
			*(gpioReg + GPPUD) = pud;
			myGpioDelay(1);
			*(gpioReg + GPPUDCLK0 + BANK) = BIT;
			myGpioDelay(1);
			*(gpioReg + GPPUD) = 0;
			*(gpioReg + GPPUDCLK0 + BANK) = 0;
			*/
			mmapIntBuffer.put(GPPUD, pull);
			SleepUtil.busySleep(1_000);
			mmapIntBuffer.put(GPIO_TO_PUDCLK[gpio], 1 << (gpio & 0x1F));
			SleepUtil.busySleep(1_000);
			mmapIntBuffer.put(GPPUD, 0);
			mmapIntBuffer.put(GPIO_TO_PUDCLK[gpio], 0);
		}
	}

	@Override
	public boolean gpioRead(int gpio) {
		// return (gpioReg.get(GPLEV0 + (gpio >> 5)) & (1 << (gpio & 0x1F))) != 0;
		// return (mmapIntBuffer.get(GPIO_TO_GPLEV[gpio]) & (1 << (gpio & 0x1F))) != 0;
		return mmapIntBuffer.get(GPIO_TO_GPLEV[gpio], 1 << (gpio & 0x1F)) != 0;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		if (value) {
			// pigpio
			// gpioReg.put(GPSET0 + gpio >> 5, 1 << (gpio & 0x1F));
			// wiringPi
			mmapIntBuffer.put(GPIO_TO_GPSET[gpio], 1 << (gpio & 0x1F));
		} else {
			// pigpio
			// gpioReg.put(GPCLR0 + gpio >> 5, 1 << (gpio & 0x1F));
			// wiringPi
			mmapIntBuffer.put(GPIO_TO_GPCLR[gpio], 1 << (gpio & 0x1F));
		}
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		String soc = RaspberryPiBoardInfoProvider.BCM2711;
		if (args.length > 0) {
			soc = args[0];
		}
		boolean PI_OFF = false;
		boolean PI_ON = true;

		try (BroadcomMmapGpio mmap = new BroadcomMmapGpio(soc)) {
			mmap.initialise();

			int gpio = 21;
			for (int i = 0; i < 10; i++) {
				mmap.setMode(gpio, DeviceMode.DIGITAL_INPUT);
				System.out.format("Mode for GPIO# %d: %s, value: %b. (Mode should be %s)%n", gpio, mmap.getMode(gpio),
						mmap.gpioRead(gpio), DeviceMode.DIGITAL_INPUT);
				mmap.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
				System.out.format("Mode for GPIO# %d: %s, value: %b. (Mode should be %s)%n", gpio, mmap.getMode(gpio),
						mmap.gpioRead(gpio), DeviceMode.DIGITAL_OUTPUT);
				mmap.gpioWrite(gpio, PI_ON);
				System.out.format("Mode for GPIO# %d: %s, value: %b. (Value should be %b)%n", gpio, mmap.getMode(gpio),
						mmap.gpioRead(gpio), PI_ON);
				SleepUtil.sleepSeconds(1);
				mmap.gpioWrite(gpio, PI_OFF);
				System.out.format("Mode for GPIO# %d: %s, value: %b. (Value should be %b)%n", gpio, mmap.getMode(gpio),
						mmap.gpioRead(gpio), PI_OFF);
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
