package com.diozero.internal.soc.allwinner;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AllwinnerHMmapGpio.java
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/*-
 * Supports Allwinner H5 & H6 System on Chips
 * 
 * Register  Offset       Description
 * Pn_CFG0   n*0x24+0x00  Port n Configure Register 0     Pn0..7_CFG (3+1 bits per GPIO)
 * Pn_CFG1   n*0x24+0x04  Port n Configure Register 1     Pn8..15_CFG (3+1 bits per GPIO)
 * Pn_CFG2   n*0x24+0x08  Port n Configure Register 2     Pn16..23_CFG (3+1 bits per GPIO)
 * Pn_CFG3   n*0x24+0x0c  Port n Configure Register 3     Pn24..31_CFG (3+1 bits per GPIO)
 * Pn_DAT    n*0x24+0x10  Port n Data Register            Pn_DAT (1 bit per GPIO)
 * Pn_DRV0   n*0x24+0x14  Port n Multi-Driving Register 0 Pn0..15_DRV (2 bits per GPIO)
 * Pn_DRV1   n*0x24+0x18  Port n Multi-Driving Register 1 Pn16..31_DRV (2 bits per GPIO)
 * Pn_PUL0   n*0x24+0x1c  Port n Pull Register 0          Pn0..15_PULL (2 bits per GPIO)
 * Pn_PUL1   n*0x24+0x20  Port n Pull Register 1          Pn16..31_PULL
 */
public abstract class AllwinnerHMmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/mem";

	private static final int MEM_INFO = 1024;
	private static final int BLOCK_SIZE = 4 * MEM_INFO;
	private static final int PIO_MAP_SIZE = BLOCK_SIZE;

	static final int BANK_INT_OFFSET = 0x24 / 4;

	static final int CONFIG_REG_INT_OFFSET = 0x00 / 4;
	static final int DATA_REG_INT_OFFSET = 0x10 / 4;
	// static final int DRV_REG_INT_OFFSET = 0x14 / 4;
	static final int PULL_REG_INT_OFFSET = 0x1c / 4;

	private boolean initialised;
	private MmapIntBuffer gpioAMmapIntBuffer;
	private MmapIntBuffer gpioLMmapIntBuffer;
	private final long gpioABase;
	private int gpioAIntOffset;
	private final long gpioLBase;
	private int gpioLIntOffset;
	private final int[] numGpiosByBank;
	private Map<String, DeviceMode> gpioModes;
	private Map<String, Integer> gpioModeValues;

	AllwinnerHMmapGpio(long gpioABase, int gpioAIntOffset, long gpioLBase, int gpioLIntOffset, int[] numGpiosByBank) {
		this.gpioABase = gpioABase;
		this.gpioAIntOffset = gpioAIntOffset;
		this.gpioLBase = gpioLBase;
		this.gpioLIntOffset = gpioLIntOffset;
		this.numGpiosByBank = numGpiosByBank;
	}

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			gpioAMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, gpioABase, PIO_MAP_SIZE, ByteOrder.LITTLE_ENDIAN);
			gpioLMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, gpioLBase, PIO_MAP_SIZE, ByteOrder.LITTLE_ENDIAN);

			initialised = true;
		}

		gpioModes = new HashMap<>();
		gpioModeValues = new HashMap<>();

		initialiseGpioModes();
	}

	abstract void initialiseGpioModes();

	void addGpioMode(int gpio, int value, DeviceMode mode) {
		gpioModes.put(gpio + "-" + value, mode);
		gpioModeValues.put(gpio + "-" + mode, Integer.valueOf(value));
	}

	@Override
	public synchronized void close() {
		if (initialised) {
			gpioAMmapIntBuffer.close();
			gpioAMmapIntBuffer = null;
			gpioLMmapIntBuffer.close();
			gpioLMmapIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		final int bank = gpio >> 5;
		final int bank_index = gpio % 32;
		if (numGpiosByBank[bank] < bank_index) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + ", bank " + bank + " index " + bank_index
					+ " - number of GPIOs in this bank: " + numGpiosByBank[bank]);
		}

		int mode_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 11) {
			mode_int_offset = gpioAIntOffset + CONFIG_REG_INT_OFFSET + bank * BANK_INT_OFFSET + (bank_index >> 3);
			int_buffer = gpioAMmapIntBuffer;
		} else {
			mode_int_offset = gpioLIntOffset + CONFIG_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET
					+ (bank_index >> 3);
			int_buffer = gpioLMmapIntBuffer;
		}

		final int mode_shift = (gpio % 8) << 2;
		final int mode_val = (int_buffer.get(mode_int_offset) >> mode_shift) & 0b111;
		DeviceMode mode;
		switch (mode_val) {
		case 0b000:
			mode = DeviceMode.DIGITAL_INPUT;
			break;
		case 0b001:
			mode = DeviceMode.DIGITAL_OUTPUT;
			break;
		default:
			mode = gpioModes.getOrDefault(gpio + "-" + mode_val, DeviceMode.UNKNOWN);
		}

		return mode;
	}

	@Override
	public void setMode(int gpio, DeviceMode mode) {
		int mode_val;

		switch (mode) {
		case DIGITAL_INPUT:
			mode_val = 0b000;
			break;
		case DIGITAL_OUTPUT:
			mode_val = 0b001;
			break;
		default:
			final Integer i = gpioModeValues.get(gpio + "-" + mode);
			if (i == null) {
				throw new IllegalArgumentException("Invalid mode " + mode + " for GPIO " + gpio);
			}
			mode_val = i.intValue();
		}

		setModeUnchecked(gpio, mode_val);
	}

	@Override
	public void setModeUnchecked(int gpio, int mode) {
		final int bank = gpio >> 5;
		final int bank_index = gpio % 32;
		if (numGpiosByBank[bank] < bank_index) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + ", bank " + bank + " index " + bank_index
					+ " - number of GPIOs in this bank: " + numGpiosByBank[bank]);
		}

		int mode_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 11) {
			mode_int_offset = gpioAIntOffset + CONFIG_REG_INT_OFFSET + bank * BANK_INT_OFFSET + (bank_index >> 3);
			int_buffer = gpioAMmapIntBuffer;
		} else {
			mode_int_offset = gpioLIntOffset + CONFIG_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET
					+ (bank_index >> 3);
			int_buffer = gpioLMmapIntBuffer;
		}

		final int mode_shift = (gpio % 8) << 2;
		int reg_val = int_buffer.get(mode_int_offset);
		reg_val &= ~(0b111 << mode_shift);
		reg_val |= ((mode & 0b111) << mode_shift);

		int_buffer.put(mode_int_offset, reg_val);
	}

	@Override
	public Optional<GpioPullUpDown> getPullUpDown(int gpio) {
		final int bank = gpio >> 5; // equivalent to / 32
		final int bank_index = gpio % 32;
		if (numGpiosByBank[bank] < bank_index) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + ", bank " + bank + " index " + bank_index
					+ " - number of GPIOs in this bank: " + numGpiosByBank[bank]);
		}

		final int pud_shift = (gpio % 16) << 1; // Shift 0..30
		int pud_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 11) {
			pud_int_offset = gpioAIntOffset + PULL_REG_INT_OFFSET + bank * BANK_INT_OFFSET + (bank_index >> 4);
			int_buffer = gpioAMmapIntBuffer;
		} else {
			pud_int_offset = gpioLIntOffset + PULL_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET + (bank_index >> 4);
			int_buffer = gpioLMmapIntBuffer;
		}

		GpioPullUpDown pud;
		switch (int_buffer.get(pud_int_offset) >> pud_shift & 0b11) {
		case 0b10:
			pud = GpioPullUpDown.PULL_UP;
			break;
		case 0b01:
			pud = GpioPullUpDown.PULL_UP;
			break;
		default:
			pud = GpioPullUpDown.NONE;
		}

		return Optional.of(pud);
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		/*-
		 * Pull Registers
		 * 00: Disable, 01: Pull-up, 10: Pull-down
		 */
		final int bank = gpio >> 5; // equivalent to / 32
		final int bank_index = gpio % 32;
		if (numGpiosByBank[bank] < bank_index) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + ", bank " + bank + " index " + bank_index
					+ " - number of GPIOs in this bank: " + numGpiosByBank[bank]);
		}

		int pud_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 11) {
			pud_int_offset = gpioAIntOffset + PULL_REG_INT_OFFSET + bank * BANK_INT_OFFSET + (bank_index >> 4);
			int_buffer = gpioAMmapIntBuffer;
		} else {
			pud_int_offset = gpioLIntOffset + PULL_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET + (bank_index >> 4);
			int_buffer = gpioLMmapIntBuffer;
		}

		int pud_val;
		switch (pud) {
		case PULL_UP:
			pud_val = 0b10;
			break;
		case PULL_DOWN:
			pud_val = 0b01;
			break;
		case NONE:
		default:
			pud_val = 0b00;
		}

		final int pud_shift = (gpio % 16) << 1; // Shift 0..30
		int reg_val = int_buffer.get(pud_int_offset);
		reg_val &= ~(0b11 << pud_shift);
		reg_val |= (pud_val << pud_shift);

		int_buffer.put(pud_int_offset, reg_val);

		SleepUtil.sleepMillis(1);
	}

	@Override
	public boolean gpioRead(int gpio) {
		final int bank = gpio >> 5;
		final int bank_index = gpio % 32;
		if (numGpiosByBank[bank] < bank_index) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + ", bank " + bank + " index " + bank_index
					+ " - number of GPIOs in this bank: " + numGpiosByBank[bank]);
		}

		int data_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 11) {
			data_int_offset = gpioAIntOffset + DATA_REG_INT_OFFSET + bank * BANK_INT_OFFSET;
			int_buffer = gpioAMmapIntBuffer;
		} else {
			data_int_offset = gpioLIntOffset + DATA_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET;
			int_buffer = gpioLMmapIntBuffer;
		}

		final int data_shift = bank_index;
		return ((int_buffer.get(data_int_offset) >> data_shift) & 1) == 1;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		final int bank = gpio >> 5;
		final int bank_index = gpio % 32;
		if (numGpiosByBank[bank] < bank_index) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + ", bank " + bank + " index " + bank_index
					+ " - number of GPIOs in this bank: " + numGpiosByBank[bank]);
		}

		int data_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 11) {
			data_int_offset = gpioAIntOffset + DATA_REG_INT_OFFSET + bank * BANK_INT_OFFSET;
			int_buffer = gpioAMmapIntBuffer;
		} else {
			data_int_offset = gpioLIntOffset + DATA_REG_INT_OFFSET + (bank - 11) * BANK_INT_OFFSET;
			int_buffer = gpioLMmapIntBuffer;
		}

		final int data_shift = bank_index;
		int reg_val = int_buffer.get(data_int_offset);
		if (value) {
			reg_val |= (1 << data_shift);
		} else {
			reg_val &= ~(1 << data_shift);
		}

		int_buffer.put(data_int_offset, reg_val);
	}
}
