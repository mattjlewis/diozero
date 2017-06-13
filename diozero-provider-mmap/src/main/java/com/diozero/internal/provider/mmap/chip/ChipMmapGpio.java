package com.diozero.internal.provider.mmap.chip;

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
import com.diozero.internal.provider.mmap.MmapGpioInterface;
import com.diozero.util.*;

// Register mapping courtesy of WereCatf
//https://bbs.nextthing.co/t/chippy-gonzales-fast-gpio/14056/6?u=xtacocorex
// https://github.com/WereCatf/Gonzales
// https://github.com/LuciferAndDiablo/NTC-C.H.I.P.-JavaGPIOLib
// Refer to the Allwinner R8 User Manual V1.1 datasheet, section 32.2
// Modes: 0 == INPUT, 1 == OUTPUT, 2-7 == alt functions
public class ChipMmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";
	private static final int GPIO_BASE_OFFSET = 0x01c20000;
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
		PORT_CONFIGS[5] = new PortConfig(5, 6);  // F
		PORT_CONFIGS[6] = new PortConfig(6, 13); // G
	}
	private static final int PUD_DISABLE = 0b00;
	private static final int PUD_PULL_UP = 0b01;
	private static final int PUD_PULL_DOWN = 0b10;
	
	private MmapByteBuffer mmap;
	private IntBuffer gpioIntBuffer;
	
	@Override
	public synchronized void initialise() {
		if (mmap == null) {
			mmap = MmapBufferNative.createMmapBuffer(MEM_DEVICE, GPIO_BASE_OFFSET, 2*PAGE_SIZE);
			gpioIntBuffer = mmap.getBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
		}
	}
	
	@Override
	public synchronized void close() {
		if (mmap != null) {
			MmapBufferNative.closeMmapBuffer(mmap.getFd(), mmap.getAddress(), mmap.getLength());
		}
	}
	
	@Override
	public DeviceMode getMode(int gpio) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		//int config_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + CONFIG_REG_INT_OFFSET + (pin>>3);
		int config_reg = PORT_CONFIGS[port].configRegisters[pin];
		int config_val = (gpioIntBuffer.get(config_reg) >> ((pin % 8) * 4)) & 0b111;
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
		int port = getPort(gpio);
		int pin = getPin(gpio);
		//int config_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + CONFIG_REG_INT_OFFSET + (pin>>3);
		int config_reg = PORT_CONFIGS[port].configRegisters[pin];
		int current_reg_val = gpioIntBuffer.get(config_reg);
		// Blank out the old bits so we can overwrite them
		int shift = (pin % 8) * 4;
		current_reg_val &= ~(0b111 << shift);
		switch (mode) {
		case DIGITAL_INPUT:
			gpioIntBuffer.put(config_reg, current_reg_val);
			break;
		case DIGITAL_OUTPUT:
			gpioIntBuffer.put(config_reg, current_reg_val | (0b001 << shift));
			break;
		case PWM_OUTPUT:
			if (gpio != 34 && gpio != 205) {
				throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
			}
			gpioIntBuffer.put(config_reg, current_reg_val | (0b010 << shift));
			break;
		default:
			throw new IllegalArgumentException("Invalid GPIO mode " + mode + " for pin " + gpio);
		}
	}
	
	public GpioPullUpDown getPullUpDown(int gpio) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		int pull_reg = PORT_CONFIGS[port].pullRegisters[pin];
		int shift = (pin % 16) * 2;
		GpioPullUpDown pud;
		switch ((gpioIntBuffer.get(pull_reg) >> shift) & 0b11) {
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
		
		return pud;
	}
	
	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		int pull_reg = PORT_CONFIGS[port].pullRegisters[pin];
		int current_reg_val = gpioIntBuffer.get(pull_reg);
		// Blank out the old bits so we can overwrite them
		int shift = (pin % 16) * 2;
		current_reg_val &= ~(0b11 << shift);
		switch (pud) {
		case NONE:
			gpioIntBuffer.put(pull_reg, current_reg_val);
			break;
		case PULL_UP:
			gpioIntBuffer.put(pull_reg, current_reg_val | (PUD_PULL_UP << shift));
			break;
		case PULL_DOWN:
			gpioIntBuffer.put(pull_reg, current_reg_val | (PUD_PULL_DOWN << shift));
			break;
		}
	}
	
	@Override
	public boolean gpioRead(int gpio) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		
		//int data_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + DATA_REG_INT_OFFSET;
		return ((gpioIntBuffer.get(PORT_CONFIGS[port].dataRegister) >> pin) & 1) == 1;
	}
	
	@Override
	public void gpioWrite(int gpio, boolean value) {
		int port = getPort(gpio);
		int pin = getPin(gpio);
		
		//int data_reg = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + DATA_REG_INT_OFFSET;
		int data_reg = PORT_CONFIGS[port].dataRegister;
		if (value) {
			gpioIntBuffer.put(data_reg, gpioIntBuffer.get(data_reg) | (1 << pin));
		} else {
			gpioIntBuffer.put(data_reg, gpioIntBuffer.get(data_reg) & ~(1 << pin));
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
			for (int pin=0; pin<pins; pin++) {
				configRegisters[pin] = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + CONFIG_REG_INT_OFFSET + (pin>>3);
			}
			dataRegister = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + DATA_REG_INT_OFFSET;
			pullRegisters = new int[pins];
			for (int pin=0; pin<pins; pin++) {
				pullRegisters[pin] = PIO_START_INT_OFFSET + port*PORT_INT_OFFSET + PULL_REG_INT_OFFSET + (pin>>4);
			}
		}
	}
	
	public static void main(String[] args) {
		LibraryLoader.loadLibrary(ChipMmapGpio.class, "diozero-system-utils");
		try (ChipMmapGpio mmap_gpio = new ChipMmapGpio()) {
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
			for (int i=0; i<iterations; i++) {
				mmap_gpio.gpioWrite(perf_test_gpio, true);
				mmap_gpio.gpioWrite(perf_test_gpio, false);
			}
			long duration = System.currentTimeMillis() - start_ms;
			System.out.println("Took " + duration + "ms for " + iterations);
			mmap_gpio.setMode(perf_test_gpio, mode);
			
			while (true) {
				mode = mmap_gpio.getMode(gpio);
				System.out.println("Mode: " + mode);
				if (mode == DeviceMode.DIGITAL_OUTPUT) {
					on = mmap_gpio.gpioRead(gpio);
					System.out.println("On: " + on);
				} else if (mode == DeviceMode.DIGITAL_INPUT) {
					GpioPullUpDown pud = mmap_gpio.getPullUpDown(gpio);
					System.out.println("PUD: " + pud);
				}
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
