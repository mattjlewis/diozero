package com.diozero.devices.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Max30102.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;

public class Max30102 {
	public static enum SampleAveraging {
		_1(0b000), _2(0b001), _4(0b010), _8(0b011), _16(0b100), _32(0b101);

		private static final int BIT_SHIFT = 5;

		private byte mask;

		private SampleAveraging(int val) {
			this.mask = (byte) (val << BIT_SHIFT);
		}

		public byte getMask() {
			return mask;
		}
	}

	public static enum FifoRolloverOnFull {
		ENABLED(1), DISABLED(0);

		private static final int BIT_SHIFT = 4;

		private byte mask;

		private FifoRolloverOnFull(int val) {
			this.mask = (byte) (val << BIT_SHIFT);
		}

		public byte getMask() {
			return mask;
		}
	}

	private static int DEVICE_ADDRESS = 0x57;

	private static final int REG_INTR_STATUS_1 = 0x00;
	private static final int REG_INTR_STATUS_2 = 0x01;
	private static final int REG_INTR_ENABLE_1 = 0x02;
	private static final int REG_INTR_ENABLE_2 = 0x03;
	private static final int REG_FIFO_WRITE_PTR = 0x04;
	private static final int REG_OVERFLOW_CTR = 0x05;
	private static final int REG_FIFO_READ_PTR = 0x06;
	private static final int REG_FIFO_DATA = 0x07;
	private static final int REG_FIFO_CONFIG = 0x08;
	private static final int REG_MODE_CONFIG = 0x09;
	private static final int REG_SPO2_CONFIG = 0x0a;
	private static final int REG_LED1_PULSE_AMPL = 0x0c;
	private static final int REG_LED2_PULSE_AMPL = 0x0d;
	private static final int REG_MULTI_LED_CTRL1 = 0x11;
	private static final int REG_MULTI_LED_CTRL2 = 0x12;
	private static final int REG_DIE_TEMP_INT = 0x1f;
	private static final int REG_DIE_TEMP_FRC = 0x20;
	private static final int REG_DIE_TEMP_CONFIG = 0x21;
	private static final int REG_REVISION_ID = 0xfe;
	private static final int REG_PART_ID = 0xff;

	// Interrupt Status #1

	// In SpO2 and HR modes, this interrupt triggers when the FIFO write pointer has
	// a certain number of free spaces remaining.
	// The interrupt is cleared by reading the Interrupt Status 1 register (0x00).
	private static final int FIFO_ALMOST_FULL_BIT = 7;
	private static final int FIFO_ALMOST_FULL_MASK = 1 << FIFO_ALMOST_FULL_BIT;
	// In SpO2 and HR modes, this interrupt triggers when there is a new sample in
	// the data FIFO.
	// The interrupt is cleared by reading the Interrupt Status 1 register (0x00),
	// or by reading the FIFO_DATA register.
	private static final int NEW_FIFO_DATA_BIT = 6;
	private static final int NEW_FIFO_DATA_MASK = 1 << NEW_FIFO_DATA_BIT;
	// Ambient Light Cancellation Overflow
	// This interrupt triggers when the ambient light cancellation function of the
	// SpO2/HR photodiode has reached its maximum limit, and therefore, ambient
	// light is affecting the output of the ADC.
	// The interrupt is cleared by reading the Interrupt Status 1 register (0x00).
	private static final int ALC_OVF_BIT = 5;
	private static final int ALC_OVF_MASK = 1 << ALC_OVF_BIT;
	// On power-up a power-ready interrupt is triggered to signal that the module is
	// powered-up and ready to collect data.
	private static final int POWER_READY_BIT = 0;
	private static final int POWER_READY_MASK = 1 << POWER_READY_BIT;

	// Interrupt Status #2

	// Internal Temperature Ready Flag
	// When an internal die temperature conversion is finished, this interrupt is
	// triggered so the processor can read the temperature data registers.
	// The interrupt is cleared by reading either the Interrupt Status 2 register
	// (0x01) or the TFRAC register (0x20).
	private static final int DIE_TEMP_RDY_BIT = 1;
	private static final int DIE_TEMP_RDY_MASK = 1 << DIE_TEMP_RDY_BIT;

	private I2CDevice device;

	public Max30102() {
		this(I2CConstants.CONTROLLER_0);
	}

	public Max30102(int controller) {
		device = I2CDevice.builder(DEVICE_ADDRESS).setController(controller).build();

		reset();
	}

	public void reset() {
		device.writeByteData(REG_MODE_CONFIG, 0x40);
	}

	public byte getRevisionId() {
		return device.readByteData(REG_REVISION_ID);
	}

	public byte getPartId() {
		return device.readByteData(REG_PART_ID);
	}

	public void setup(SampleAveraging sampleAveraging, FifoRolloverOnFull fifoRolloverOnFull,
			int freeFifoEntriesInterruptThreshold) {
		// Interrupt settings
		device.writeByteData(REG_INTR_ENABLE_1, FIFO_ALMOST_FULL_MASK | NEW_FIFO_DATA_MASK | ALC_OVF_MASK);
		device.writeByteData(REG_INTR_ENABLE_2, DIE_TEMP_RDY_MASK);

		// FIFO_WR_PTR[4:0]
		device.writeByteData(REG_FIFO_WRITE_PTR, 0);
		// FIFO_RD_PTR[4:0]
		device.writeByteData(REG_FIFO_READ_PTR, 0);
		// OVF_COUNTER[4:0]
		device.writeByteData(REG_OVERFLOW_CTR, 0);

		// Sample avg = 4, FIFO rollover = false, fifo almost full = 17
		device.writeByteData(REG_FIFO_CONFIG,
				sampleAveraging.getMask() | fifoRolloverOnFull.getMask() | freeFifoEntriesInterruptThreshold);
		device.writeByteData(REG_FIFO_CONFIG, 0b0100_1111);

		// 0x02 for read-only, 0x03 for SpO2 mode, 0x07 multimode LED
		device.writeByteData(REG_MODE_CONFIG, 0x03);

		// SPO2_ADC range = 4096nA, SPO2 sample rate = 100Hz, LED pulse-width = 411uS
		device.writeByteData(REG_SPO2_CONFIG, 0b0010_0111);

		// Choose value for ~7mA for LED1
		device.writeByteData(REG_LED1_PULSE_AMPL, 0x24);
		// Choose value for ~7mA for LED2
		device.writeByteData(REG_LED2_PULSE_AMPL, 0x24);
		// Choose value for ~25mA for Pilot LED
		// device.writeByteData(REG_PILOT_PULSE_AMPL, 0x7f);
	}

	public int getDataPresent() {
		int read_ptr = device.readByteData(REG_FIFO_READ_PTR) & 0xff;
		int write_ptr = device.readByteData(REG_FIFO_WRITE_PTR) & 0xff;

		int num_samples = write_ptr - read_ptr;
		if (num_samples < 0) {
			num_samples += 32;
		}

		return num_samples;
	}

	public void readFifo() {
		// Reading the interrupt registers clears the interrupt status
		byte intr_1 = device.readByteData(REG_INTR_STATUS_1);
		byte intr_2 = device.readByteData(REG_INTR_STATUS_2);

		byte[] data = device.readI2CBlockDataByteArray(REG_FIFO_DATA, 6);

		int red_led = (data[0] << 16 | data[1] << 8 | data[2]) & 0x03FFFF;
		int ir_led = (data[3] << 16 | data[4] << 8 | data[5]) & 0x03FFFF;
	}
}
