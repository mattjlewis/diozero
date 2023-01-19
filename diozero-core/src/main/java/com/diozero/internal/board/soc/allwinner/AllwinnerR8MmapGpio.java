package com.diozero.internal.board.soc.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerR8MmapGpio.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InvalidModeException;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.LibraryLoader;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

// Register mapping courtesy of WereCatf
//https://bbs.nextthing.co/t/chippy-gonzales-fast-gpio/14056/6?u=xtacocorex
// https://github.com/WereCatf/Gonzales
// https://github.com/LuciferAndDiablo/NTC-C.H.I.P.-JavaGPIOLib
// Refer to the Allwinner R8 User Manual V1.1 datasheet, section 32.2
// Modes: 0 == INPUT, 1 == OUTPUT, 2-7 == alt functions
public class AllwinnerR8MmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";
	private static final long GPIO_BASE_OFFSET = 0x01c2_0000L;
	private static final int PAGE_SIZE = 0x1000;
	private static final int PIO_START_INT_OFFSET = 0x800 / 4;
	private static final int PORT_INT_OFFSET = 0x24 / 4;
	private static final int CONFIG_REG_INT_OFFSET = 0x00 / 4;
	private static final int DATA_REG_INT_OFFSET = 0x10 / 4;
	private static final int PULL_REG_INT_OFFSET = 0x1c / 4;
	private static final PortConfig[] PORT_CONFIGS = new PortConfig[7];
	static {
		PORT_CONFIGS[1] = new PortConfig(1, 19); // B
		PORT_CONFIGS[2] = new PortConfig(2, 20); // C
		PORT_CONFIGS[3] = new PortConfig(3, 28); // D
		PORT_CONFIGS[4] = new PortConfig(4, 12); // E
		PORT_CONFIGS[5] = new PortConfig(5, 6); // F
		PORT_CONFIGS[6] = new PortConfig(6, 13); // G
	}
	private static final int PUD_DISABLE = 0b00;
	private static final int PUD_PULL_UP = 0b01;
	private static final int PUD_PULL_DOWN = 0b10;

	// private MmapByteBuffer mmap;
	// private IntBuffer gpioIntBuffer;
	private MmapIntBuffer mmapIntBuffer;

	@Override
	public synchronized void initialise() {
		if (mmapIntBuffer == null) {
			// mmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, GPIO_BASE_OFFSET,
			// 2*PAGE_SIZE);
			// gpioIntBuffer =
			// mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
			mmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, GPIO_BASE_OFFSET, 2 * PAGE_SIZE, ByteOrder.LITTLE_ENDIAN);
		}
	}

	@Override
	public synchronized void close() {
		if (mmapIntBuffer != null) {
			mmapIntBuffer.close();
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		// int config_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET +
		// CONFIG_REG_INT_OFFSET + (pin>>3);
		int config_reg = PORT_CONFIGS[port].configRegisters[pin];
		int config_val = (mmapIntBuffer.get(config_reg) >> ((pin % 8) * 4)) & 0b111;
		switch (config_val) {
		case 0b000:
			return DeviceMode.DIGITAL_INPUT;
		case 0b001:
			return DeviceMode.DIGITAL_OUTPUT;
		case 0b010:
			if (gpio == 34 || gpio == 205) {
				return DeviceMode.PWM_OUTPUT;
			}
			return DeviceMode.UNKNOWN;
		default:
			return DeviceMode.UNKNOWN;
		}
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			// mmapIntBuffer.put(config_reg, current_reg_val);
			setModeUnchecked(gpio, 0b000);
			break;
		case DIGITAL_OUTPUT:
			// mmapIntBuffer.put(config_reg, current_reg_val | (0b001 << shift));
			setModeUnchecked(gpio, 0b001);
			break;
		case PWM_OUTPUT:
			if (gpio != 34 && gpio != 205) {
				throw new InvalidModeException("Invalid GPIO mode " + mode + " for pin " + gpio);
			}
			// mmapIntBuffer.put(config_reg, current_reg_val | (0b010 << shift));
			setModeUnchecked(gpio, 0b010);
			break;
		default:
			throw new InvalidModeException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}

	@Override
	public void setModeUnchecked(int gpio, int mode) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		// int config_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET +
		// CONFIG_REG_INT_OFFSET + (pin>>3);
		int config_reg = PORT_CONFIGS[port].configRegisters[pin];
		int current_reg_val = mmapIntBuffer.get(config_reg);
		// Blank out the old bits so we can overwrite them
		int shift = (pin % 8) * 4;
		current_reg_val &= ~(0b111 << shift);

		mmapIntBuffer.put(config_reg, current_reg_val | (mode << shift));
	}

	@Override
	public Optional<GpioPullUpDown> getPullUpDown(int gpio) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		int pull_reg = PORT_CONFIGS[port].pullRegisters[pin];
		int shift = (pin % 16) * 2;
		GpioPullUpDown pud;
		switch ((mmapIntBuffer.get(pull_reg) >> shift) & 0b11) {
		case PUD_PULL_UP:
			pud = GpioPullUpDown.PULL_UP;
			break;
		case PUD_PULL_DOWN:
			pud = GpioPullUpDown.PULL_DOWN;
			break;
		case PUD_DISABLE:
		default:
			pud = GpioPullUpDown.NONE;
			break;
		}

		return Optional.of(pud);
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		int pull_reg = PORT_CONFIGS[port].pullRegisters[pin];
		int current_reg_val = mmapIntBuffer.get(pull_reg);
		// Blank out the old bits so we can overwrite them
		int shift = (pin % 16) * 2;
		current_reg_val &= ~(0b11 << shift);
		switch (pud) {
		case NONE:
			mmapIntBuffer.put(pull_reg, current_reg_val);
			break;
		case PULL_UP:
			mmapIntBuffer.put(pull_reg, current_reg_val | (PUD_PULL_UP << shift));
			break;
		case PULL_DOWN:
			mmapIntBuffer.put(pull_reg, current_reg_val | (PUD_PULL_DOWN << shift));
			break;
		}
	}

	@Override
	public boolean gpioRead(int gpio) {
		int port = getPort(gpio);
		int pin = getPin(gpio);

		// int data_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET +
		// DATA_REG_INT_OFFSET;
		return ((mmapIntBuffer.get(PORT_CONFIGS[port].dataRegister) >> pin) & 1) == 1;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		int port = getPort(gpio);
		int pin = getPin(gpio);

		// int data_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET +
		// DATA_REG_INT_OFFSET;
		int data_reg = PORT_CONFIGS[port].dataRegister;
		if (value) {
			mmapIntBuffer.put(data_reg, mmapIntBuffer.get(data_reg) | (1 << pin));
		} else {
			mmapIntBuffer.put(data_reg, mmapIntBuffer.get(data_reg) & ~(1 << pin));
		}
	}

	private static int getPort(int gpio) {
		return gpio / 32;
	}

	private static int getPin(int gpio) {
		return gpio % 32;
	}

	private static final class PortConfig {
		int[] configRegisters;
		int dataRegister;
		int[] pullRegisters;

		public PortConfig(int port, int pins) {
			configRegisters = new int[pins];
			for (int pin = 0; pin < pins; pin++) {
				configRegisters[pin] = PIO_START_INT_OFFSET + port * PORT_INT_OFFSET + CONFIG_REG_INT_OFFSET
						+ (pin >> 3);
			}
			dataRegister = PIO_START_INT_OFFSET + port * PORT_INT_OFFSET + DATA_REG_INT_OFFSET;
			pullRegisters = new int[pins];
			for (int pin = 0; pin < pins; pin++) {
				pullRegisters[pin] = PIO_START_INT_OFFSET + port * PORT_INT_OFFSET + PULL_REG_INT_OFFSET + (pin >> 4);
			}
		}
	}

	public static void main(String[] args) {
		LibraryLoader.loadSystemUtils();
		try (AllwinnerR8MmapGpio mmap_gpio = new AllwinnerR8MmapGpio()) {
			mmap_gpio.initialise();
			int gpio = 135; // PE7, CSID3
			DeviceMode mode;
			boolean on;

			mode = mmap_gpio.getMode(gpio);
			System.out.println("Mode: " + mode);
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			mode = mmap_gpio.getMode(gpio);
			System.out.println("Mode: " + mode);
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_INPUT);
			mode = mmap_gpio.getMode(gpio);
			System.out.println("Mode: " + mode);
			mmap_gpio.setMode(gpio, DeviceMode.DIGITAL_OUTPUT);
			mode = mmap_gpio.getMode(gpio);
			System.out.println("Mode: " + mode);
			on = mmap_gpio.gpioRead(gpio);
			System.out.println("On: " + on);
			mmap_gpio.gpioWrite(gpio, !on);
			on = mmap_gpio.gpioRead(gpio);
			System.out.println("On: " + on);
			mmap_gpio.gpioWrite(gpio, !on);
			on = mmap_gpio.gpioRead(gpio);
			System.out.println("On: " + on);

			int perf_test_gpio = 121; // PD25
			mode = mmap_gpio.getMode(perf_test_gpio);
			mmap_gpio.setMode(perf_test_gpio, DeviceMode.DIGITAL_OUTPUT);
			long start_ms = System.currentTimeMillis();
			int iterations = 4_000_000;
			for (int i = 0; i < iterations; i++) {
				mmap_gpio.gpioWrite(perf_test_gpio, true);
				mmap_gpio.gpioWrite(perf_test_gpio, false);
			}

			long duration_ms = System.currentTimeMillis() - start_ms;
			double frequency = iterations / (duration_ms / 1000.0);

			System.out.format("Duration for %,d iterations: %,.3f s, frequency: %,.0f Hz%n",
					Integer.valueOf(iterations), Float.valueOf(((float) duration_ms) / 1000),
					Double.valueOf(frequency));

			mmap_gpio.setMode(perf_test_gpio, mode);

			while (true) {
				mode = mmap_gpio.getMode(gpio);
				System.out.println("Mode: " + mode);
				if (mode == DeviceMode.DIGITAL_OUTPUT) {
					on = mmap_gpio.gpioRead(gpio);
					System.out.println("On: " + on);
				} else if (mode == DeviceMode.DIGITAL_INPUT) {
					GpioPullUpDown pud = mmap_gpio.getPullUpDown(gpio).get();
					System.out.println("PUD: " + pud);
				}
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
